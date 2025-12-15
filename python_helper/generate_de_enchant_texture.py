"""
Generate the de-enchant item texture for the Leo Enchants mod.
Creates a 16x16 pixel texture with a digital/glitchy appearance.
"""

from PIL import Image, ImageDraw
import os

def generate_de_enchant_texture():
    # Create a 16x16 RGBA image
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    
    # Color palette - dark purple/void theme with digital accents
    void_dark = (20, 10, 35, 255)
    void_mid = (45, 25, 70, 255)
    void_light = (80, 50, 120, 255)
    digital_green = (0, 255, 65, 255)
    digital_cyan = (0, 217, 255, 255)
    highlight = (180, 130, 255, 255)
    white_spark = (255, 255, 255, 200)
    
    # Draw the base shape - looks like a staff/wand with digital elements
    # Handle/base (bottom part)
    for y in range(11, 16):
        for x in range(7, 9):
            img.putpixel((x, y), void_dark)
    
    # Staff body (middle part)
    for y in range(5, 12):
        for x in range(7, 9):
            img.putpixel((x, y), void_mid)
    
    # Add digital lines on staff
    img.putpixel((7, 6), digital_green)
    img.putpixel((7, 8), digital_cyan)
    img.putpixel((7, 10), digital_green)
    
    # Head of the wand - crystalline shape (top part)
    # Row 1 (y=1)
    img.putpixel((7, 1), void_light)
    img.putpixel((8, 1), void_light)
    
    # Row 2 (y=2)
    for x in range(6, 10):
        img.putpixel((x, 2), void_light)
    
    # Row 3 (y=3)
    for x in range(5, 11):
        img.putpixel((x, 3), void_mid)
    
    # Row 4 (y=4)
    for x in range(5, 11):
        img.putpixel((x, 4), void_mid)
    
    # Row 5 (y=5) - narrowing
    for x in range(6, 10):
        img.putpixel((x, 5), void_dark)
    
    # Highlight on crystal
    img.putpixel((6, 3), highlight)
    img.putpixel((7, 2), highlight)
    img.putpixel((7, 3), (220, 200, 255, 255))
    
    # Digital "0" and "1" particles floating around
    img.putpixel((3, 2), digital_green)
    img.putpixel((12, 3), digital_cyan)
    img.putpixel((4, 5), digital_cyan)
    img.putpixel((11, 4), digital_green)
    img.putpixel((2, 4), digital_green)
    img.putpixel((13, 2), digital_cyan)
    
    # White sparkles
    img.putpixel((4, 1), white_spark)
    img.putpixel((11, 2), white_spark)
    img.putpixel((3, 6), white_spark)
    img.putpixel((12, 5), white_spark)
    
    return img


def main():
    # Generate the texture
    texture = generate_de_enchant_texture()
    
    # Define output path
    output_dir = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        'src', 'main', 'resources', 'assets', 'leo_enchants', 'textures', 'item'
    )
    
    # Create directory if it doesn't exist
    os.makedirs(output_dir, exist_ok=True)
    
    # Save the texture
    output_path = os.path.join(output_dir, 'de_enchant.png')
    texture.save(output_path, 'PNG')
    print(f"Generated de-enchant texture: {output_path}")
    
    # Also show the texture details
    print(f"Texture size: {texture.size}")
    print(f"Texture mode: {texture.mode}")
    print("Texture generated successfully!")


if __name__ == '__main__':
    main()
