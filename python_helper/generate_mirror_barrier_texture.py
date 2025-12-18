"""
Generate the mirror barrier item texture for the Leo Enchants mod.
Creates a 16x16 pixel texture with a magical mirror/portal appearance.
"""

from PIL import Image, ImageDraw
import os


def generate_mirror_barrier_texture():
    # Create a 16x16 RGBA image
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    
    # Color palette - ethereal mirror theme with dimensional accents
    frame_dark = (40, 35, 50, 255)        # Dark frame
    frame_mid = (70, 60, 90, 255)         # Mid frame
    frame_light = (100, 90, 130, 255)     # Light frame
    mirror_dark = (80, 120, 160, 255)     # Mirror dark blue
    mirror_mid = (120, 170, 210, 255)     # Mirror mid blue
    mirror_light = (180, 220, 255, 255)   # Mirror light/reflection
    portal_purple = (160, 80, 200, 255)   # Dimensional purple
    portal_pink = (220, 120, 255, 255)    # Dimensional pink
    sparkle = (255, 255, 255, 230)        # White sparkle
    glow_cyan = (100, 255, 255, 200)      # Cyan glow
    
    # Draw the mirror frame (outer edge)
    # Top edge
    for x in range(3, 13):
        img.putpixel((x, 2), frame_dark)
        img.putpixel((x, 3), frame_mid)
    
    # Bottom edge
    for x in range(3, 13):
        img.putpixel((x, 13), frame_mid)
        img.putpixel((x, 14), frame_dark)
    
    # Left edge
    for y in range(3, 14):
        img.putpixel((2, y), frame_dark)
        img.putpixel((3, y), frame_mid)
    
    # Right edge
    for y in range(3, 14):
        img.putpixel((12, y), frame_mid)
        img.putpixel((13, y), frame_dark)
    
    # Corner highlights
    img.putpixel((3, 3), frame_light)
    img.putpixel((12, 3), frame_light)
    img.putpixel((3, 13), frame_light)
    img.putpixel((12, 13), frame_light)
    
    # Mirror surface (inner reflective area)
    for y in range(4, 13):
        for x in range(4, 12):
            # Create gradient effect
            dist_from_top_left = abs(x - 4) + abs(y - 4)
            if dist_from_top_left < 3:
                img.putpixel((x, y), mirror_light)
            elif dist_from_top_left < 6:
                img.putpixel((x, y), mirror_mid)
            else:
                img.putpixel((x, y), mirror_dark)
    
    # Add dimensional swirl in the center
    img.putpixel((7, 7), portal_purple)
    img.putpixel((8, 7), portal_pink)
    img.putpixel((7, 8), portal_pink)
    img.putpixel((8, 8), portal_purple)
    img.putpixel((6, 8), portal_purple)
    img.putpixel((9, 8), portal_purple)
    img.putpixel((7, 9), portal_purple)
    img.putpixel((8, 9), portal_purple)
    
    # Add reflection highlights
    img.putpixel((5, 5), sparkle)
    img.putpixel((6, 5), (255, 255, 255, 180))
    img.putpixel((5, 6), (255, 255, 255, 180))
    
    # Add cyan glow particles around the mirror
    img.putpixel((1, 4), glow_cyan)
    img.putpixel((14, 5), glow_cyan)
    img.putpixel((0, 8), glow_cyan)
    img.putpixel((15, 9), glow_cyan)
    img.putpixel((2, 12), glow_cyan)
    img.putpixel((13, 11), glow_cyan)
    
    # Add sparkles at corners
    img.putpixel((1, 1), sparkle)
    img.putpixel((14, 2), sparkle)
    img.putpixel((1, 14), sparkle)
    img.putpixel((14, 14), sparkle)
    
    # Add decorative handle at bottom
    img.putpixel((7, 15), frame_dark)
    img.putpixel((8, 15), frame_dark)
    img.putpixel((7, 14), frame_mid)
    img.putpixel((8, 14), frame_mid)
    
    return img


def main():
    # Generate the texture
    texture = generate_mirror_barrier_texture()
    
    # Define output path - go up one level from python_helper to project root
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    output_dir = os.path.join(
        project_root,
        'src', 'main', 'resources', 'assets', 'leo_enchants', 'textures', 'item'
    )
    
    # Create directory if it doesn't exist
    os.makedirs(output_dir, exist_ok=True)
    
    # Save the texture
    output_path = os.path.join(output_dir, 'mirror_barrier.png')
    texture.save(output_path, 'PNG')
    print(f"Generated mirror barrier texture: {output_path}")
    
    # Also show the texture details
    print(f"Texture size: {texture.size}")
    print(f"Texture mode: {texture.mode}")
    print("Texture generated successfully!")


if __name__ == '__main__':
    main()
