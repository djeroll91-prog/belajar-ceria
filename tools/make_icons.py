import math, struct, zlib, os

SS = 4  # supersample factor


def blend(base, top):
    """Alpha-composite top (r,g,b,a) over base (r,g,b[,a])."""
    base = tuple(base) if len(base) == 4 else (base[0], base[1], base[2], 255)
    ar, ag, ab, aa = base
    tr, tg, tb, ta = top
    a = ta / 255.0 + aa / 255.0 * (1 - ta / 255.0)
    if a <= 0:
        return (0, 0, 0, 0)
    r = (tr * ta / 255.0 + ar * aa / 255.0 * (1 - ta / 255.0)) / a
    g = (tg * ta / 255.0 + ag * aa / 255.0 * (1 - ta / 255.0)) / a
    b = (tb * ta / 255.0 + ab * aa / 255.0 * (1 - ta / 255.0)) / a
    return (int(r), int(g), int(b), int(a * 255))


def lerp(a, b, t):
    return a + (b - a) * t


def rounded_rect(u, v, x0, y0, x1, y1, r):
    dx = max(x0 + r - u, u - (x1 - r), 0.0)
    dy = max(y0 + r - v, v - (y1 - r), 0.0)
    return dx * dx + dy * dy <= r * r


def ellipse(u, v, cx, cy, rx, ry):
    dx = (u - cx) / rx
    dy = (v - cy) / ry
    d = dx * dx + dy * dy
    if d <= 0.62:
        return 1.0
    if d >= 1.0:
        return 0.0
    return (1.0 - d) / 0.38


def dist(u, v, cx, cy):
    return math.hypot(u - cx, v - cy)


def sample(u, v):
    # Soft pastel background: sky blue fading into candy pink.
    top = (0x9F, 0xE8, 0xFF)
    bot = (0xFF, 0xC7, 0xE6)
    t = lerp(0.0, 1.0, v)
    col = (int(lerp(top[0], bot[0], t)), int(lerp(top[1], bot[1], t)),
           int(lerp(top[2], bot[2], t)), 255)

    # Fluffy clouds, faint so they never fight with the face.
    for cx, cy, rx, ry in ((0.20, 0.16, 0.16, 0.075), (0.83, 0.24, 0.14, 0.065),
                           (0.16, 0.82, 0.15, 0.07), (0.86, 0.78, 0.16, 0.075)):
        a = ellipse(u, v, cx, cy, rx, ry)
        if a > 0:
            col = blend(col, (255, 255, 255, int(150 * a)))

    # Little sparkle stars in the corners.
    for sx, sy, sr in ((0.135, 0.35, 0.030), (0.875, 0.44, 0.026),
                       (0.24, 0.63, 0.022), (0.78, 0.66, 0.024)):
        d = dist(u, v, sx, sy)
        if d <= sr:
            # Four point sparkle shape.
            k = abs(u - sx) + abs(v - sy)
            if d <= sr * 0.42 or k <= sr * 0.95:
                col = blend(col, (255, 255, 255, 235))

    outline = (0xB1, 0x9D, 0xDB, 255)
    head_fill = (255, 253, 246, 255)

    # Antenna: thin stalk topped with a chubby heart.
    if abs(u - 0.5) < 0.014 and 0.16 <= v <= 0.30:
        col = blend(col, (0xB1, 0x9D, 0xDB, 255))
    hx, hy, hs = 0.5, 0.115, 0.088
    if v < hy + hs * 0.55:
        # heart = two lobes plus a triangle
        lobe = min(dist(u, v, hx - hs * 0.36, hy - hs * 0.20),
                   dist(u, v, hx + hs * 0.36, hy - hs * 0.20))
        in_heart = lobe <= hs * 0.56
        if not in_heart and abs(u - hx) < hs * 0.86:
            span = hs * 0.86 * (1.0 - (v - (hy - hs * 0.15)) / (hs * 0.70))
            if 0 <= v - (hy - hs * 0.15) <= hs * 0.70 and abs(u - hx) <= span:
                in_heart = True
        if in_heart:
            col = (255, 0x7B, 0xAC, 255)

    # Big chibi head.
    if rounded_rect(u, v, 0.145, 0.225, 0.855, 0.855, 0.222):
        col = head_fill
    # Rounded outline of the head.
    if rounded_rect(u, v, 0.130, 0.210, 0.870, 0.870, 0.235) and \
            not rounded_rect(u, v, 0.148, 0.228, 0.852, 0.852, 0.219):
        col = outline

    # Rosy cheeks, drawn under the eyes but over the face.
    for cx in (0.315, 0.685):
        a = ellipse(u, v, cx, 0.625, 0.085, 0.055)
        if a > 0:
            col = blend(col, (255, 0x9E, 0xB5, int(150 * a)))

    # Big glossy eyes.
    eye = (0x33, 0x2B, 0x5E, 255)
    for ex in (0.375, 0.625):
        a = ellipse(u, v, ex, 0.445, 0.083, 0.103)
        if a > 0:
            col = blend(col, (eye[0], eye[1], eye[2], int(255 * a)))
        # Two sparkles per eye makes the classic cute look.
        s1 = ellipse(u, v, ex - 0.030, 0.408, 0.032, 0.034)
        if s1 > 0:
            col = blend(col, (255, 255, 255, int(255 * s1)))
        s2 = ellipse(u, v, ex + 0.026, 0.487, 0.017, 0.017)
        if s2 > 0:
            col = blend(col, (255, 255, 255, int(210 * s2)))

    # Small happy open smile with a tongue.
    m = ellipse(u, v, 0.5, 0.615, 0.072, 0.052)
    if m > 0:
        col = blend(col, (0x33, 0x2B, 0x5E, int(255 * m)))
    tongue = ellipse(u, v, 0.5, 0.652, 0.040, 0.026)
    if tongue > 0:
        col = blend(col, (0xFF, 0x8F, 0xA3, int(255 * tongue)))

    # Clip everything to a soft rounded square so the icon looks tidy.
    r = 0.235
    dx = max(abs(u - 0.5) - (0.5 - r), 0.0)
    dy = max(abs(v - 0.5) - (0.5 - r), 0.0)
    if dx * dx + dy * dy > r * r:
        return (0, 0, 0, 0)
    return col


def render(size):
    big = size * SS
    px = [[sample((x + 0.5) / big, (y + 0.5) / big) for x in range(big)] for y in range(big)]
    out = bytearray()
    for y in range(size):
        out.append(0)
        for x in range(size):
            r = g = b = a = 0
            for sy in range(SS):
                for sx in range(SS):
                    c = px[y * SS + sy][x * SS + sx]
                    c = c if len(c) == 4 else (c[0], c[1], c[2], 255)
                    r += c[0]; g += c[1]; b += c[2]; a += c[3]
            n = SS * SS
            out += bytes((r // n, g // n, b // n, a // n))
    return bytes(out)


def write_png(path, size, raw):
    def chunk(tag, data):
        return (struct.pack('>I', len(data)) + tag + data
                + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff))
    header = struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', header) + \
          chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(png)


base = '/workspace/project/app/src/main/res'
densities = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
for name, size in densities.items():
    folder = os.path.join(base, 'mipmap-' + name)
    os.makedirs(folder, exist_ok=True)
    raw = render(size)
    write_png(os.path.join(folder, 'ic_launcher.png'), size, raw)
    write_png(os.path.join(folder, 'ic_launcher_round.png'), size, raw)

# Larger previews for a quick visual check.
os.makedirs('/tmp/apkshare', exist_ok=True)
write_png('/tmp/apkshare/icon-preview.png', 384, render(384))
write_png('/tmp/apkshare/icon-small.png', 48, render(48))
print('cute icons generated')
