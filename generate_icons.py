import os
from PIL import Image, ImageDraw

def render_logo(size, shape="rounded_square", corner_radius=None, bar_scale_ratio=1.0, supersample=4):
    """
    Renders the Wave Bars logo at high resolution and downsamples with Lanczos.
    size: (width, height)
    shape: "rounded_square", "circle", or "square"
    """
    w, h = size
    W = w * supersample
    H = h * supersample
    
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    PURPLE = (0x9B, 0x86, 0xD6, 255)  # #9B86D6
    DARK_INK = (0x1C, 0x1C, 0x28, 255) # #1C1C28
    WHITE = (0xFF, 0xFF, 0xFF, 255)   # #FFFFFF
    ORANGE = (0xF6, 0xB9, 0x6B, 255)  # #F6B96B
    
    # Draw Background
    if shape == "circle":
        draw.ellipse([0, 0, W - 1, H - 1], fill=PURPLE)
    elif shape == "rounded_square":
        r = (corner_radius * supersample) if corner_radius else int(0.2 * W)
        draw.rounded_rectangle([0, 0, W - 1, H - 1], radius=r, fill=PURPLE)
    elif shape == "square":
        draw.rectangle([0, 0, W - 1, H - 1], fill=PURPLE)

    # Bars definition on 200x200 grid
    # x positions: 38, 59, 80, 101, 122, 143
    # width: 14
    # heights: 30, 62, 92, 52, 76, 38
    # centered on y=100
    # alternating white, orange
    bars_def = [
        (38, 30, WHITE),
        (59, 62, ORANGE),
        (80, 92, WHITE),
        (101, 52, ORANGE),
        (122, 76, WHITE),
        (143, 38, ORANGE),
    ]
    
    # Scale from 200x200 grid to W x H
    # Total bar span: x from 38 to 157 (span = 119), center = 97.5
    # y from 54 to 146 (span = 92), center = 100
    base_scale = (W / 200.0) * bar_scale_ratio
    center_x = W / 2.0
    center_y = H / 2.0
    
    outline_w = int(round(5.0 * base_scale))
    if outline_w < 1:
        outline_w = 1
        
    for x_pos, height, fill_color in bars_def:
        bar_w = 14.0 * base_scale
        bar_h = height * base_scale
        rx = bar_w / 2.0
        
        # Center x on target
        bx = center_x + (x_pos - 97.5) * base_scale
        by = center_y - (bar_h / 2.0)
        
        # Outline is 5px on 200x200. We draw rounded_rectangle with outline and width
        # In Pillow, outline expands inwards or centered; drawing filled + outline:
        draw.rounded_rectangle(
            [bx, by, bx + bar_w, by + bar_h],
            radius=rx,
            fill=fill_color,
            outline=DARK_INK,
            width=outline_w
        )
        
    return img.resize((w, h), Image.Resampling.LANCZOS)

def main():
    repo_root = os.path.dirname(os.path.abspath(__file__))
    
    # 1. 512x512 logo_512.png
    # Purple rounded square, 44px-radius-style corners, bars on top
    print("Generating logo_512.png...")
    logo_512 = render_logo((512, 512), shape="rounded_square", corner_radius=44, bar_scale_ratio=1.0)
    logo_512.save(os.path.join(repo_root, "logo_512.png"), "PNG")
    print("Saved logo_512.png")
    
    # 2. Legacy launcher icons
    # Densities: mdpi (48), hdpi (72), xhdpi (96), xxhdpi (144), xxxhdpi (192)
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    
    res_dir = os.path.join(repo_root, "app", "src", "main", "res")
    
    for folder, px in densities.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Normal legacy launcher (rounded square with proportional ~18% corner radius)
        # Using bar_scale_ratio = 0.85 so it fits cleanly inside legacy squircle bounds
        normal_icon = render_logo((px, px), shape="rounded_square", corner_radius=int(px * 0.18), bar_scale_ratio=0.82)
        normal_icon.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")
        
        # Round legacy launcher (circle)
        round_icon = render_logo((px, px), shape="circle", bar_scale_ratio=0.78)
        round_icon.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")
        
        print(f"Generated {folder}: {px}x{px} normal + round")

if __name__ == "__main__":
    main()
