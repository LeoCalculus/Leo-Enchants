"""
Generates the Herobrine Minecraft player skin.
- Pure black body (all body parts)
- Glowing white eyes (the iconic Herobrine look)

Requirements: pip install Pillow

Output: src/main/resources/assets/leo_enchants/textures/entity/herobrine.png
"""

from PIL import Image, ImageDraw

def create_herobrine_skin():
    # Minecraft player skin dimensions
    width, height = 64, 64
    
    # Create a new RGBA image (with alpha channel)
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Pure black color with full opacity
    black = (0, 0, 0, 255)
    dark_black = (5, 5, 5, 255)  # Slightly off-black for depth on some parts
    
    # Glowing white for eyes (the iconic Herobrine white eyes)
    white_glow = (255, 255, 255, 255)
    white_bright = (255, 255, 255, 255)
    
    # === HEAD (front, back, top, bottom, left, right) ===
    # Head top: 8-16 x, 0-8 y
    draw.rectangle([8, 0, 15, 7], fill=black)
    # Head bottom: 16-24 x, 0-8 y  
    draw.rectangle([16, 0, 23, 7], fill=black)
    # Head right: 0-8 x, 8-16 y
    draw.rectangle([0, 8, 7, 15], fill=black)
    # Head front: 8-16 x, 8-16 y
    draw.rectangle([8, 8, 15, 15], fill=black)
    # Head left: 16-24 x, 8-16 y
    draw.rectangle([16, 8, 23, 15], fill=black)
    # Head back: 24-32 x, 8-16 y
    draw.rectangle([24, 8, 31, 15], fill=black)
    
    # === HAT/OVERLAY LAYER (second layer) ===
    # Hat top: 40-48 x, 0-8 y
    draw.rectangle([40, 0, 47, 7], fill=black)
    # Hat bottom: 48-56 x, 0-8 y
    draw.rectangle([48, 0, 55, 7], fill=black)
    # Hat right: 32-40 x, 8-16 y
    draw.rectangle([32, 8, 39, 15], fill=black)
    # Hat front: 40-48 x, 8-16 y
    draw.rectangle([40, 8, 47, 15], fill=black)
    # Hat left: 48-56 x, 8-16 y
    draw.rectangle([48, 8, 55, 15], fill=black)
    # Hat back: 56-64 x, 8-16 y
    draw.rectangle([56, 8, 63, 15], fill=black)
    
    # === BODY ===
    # Body top: 20-28 x, 16-20 y
    draw.rectangle([20, 16, 27, 19], fill=black)
    # Body bottom: 28-36 x, 16-20 y
    draw.rectangle([28, 16, 35, 19], fill=black)
    # Body right: 16-20 x, 20-32 y
    draw.rectangle([16, 20, 19, 31], fill=black)
    # Body front: 20-28 x, 20-32 y
    draw.rectangle([20, 20, 27, 31], fill=black)
    # Body left: 28-32 x, 20-32 y
    draw.rectangle([28, 20, 31, 31], fill=black)
    # Body back: 32-40 x, 20-32 y
    draw.rectangle([32, 20, 39, 31], fill=black)
    
    # === RIGHT ARM ===
    # Right arm top: 44-48 x, 16-20 y
    draw.rectangle([44, 16, 47, 19], fill=black)
    # Right arm bottom: 48-52 x, 16-20 y
    draw.rectangle([48, 16, 51, 19], fill=black)
    # Right arm right: 40-44 x, 20-32 y
    draw.rectangle([40, 20, 43, 31], fill=black)
    # Right arm front: 44-48 x, 20-32 y
    draw.rectangle([44, 20, 47, 31], fill=black)
    # Right arm left: 48-52 x, 20-32 y
    draw.rectangle([48, 20, 51, 31], fill=black)
    # Right arm back: 52-56 x, 20-32 y
    draw.rectangle([52, 20, 55, 31], fill=black)
    
    # === LEFT ARM (1.8+ format, bottom half) ===
    # Left arm top: 36-40 x, 48-52 y
    draw.rectangle([36, 48, 39, 51], fill=black)
    # Left arm bottom: 40-44 x, 48-52 y
    draw.rectangle([40, 48, 43, 51], fill=black)
    # Left arm right: 32-36 x, 52-64 y
    draw.rectangle([32, 52, 35, 63], fill=black)
    # Left arm front: 36-40 x, 52-64 y
    draw.rectangle([36, 52, 39, 63], fill=black)
    # Left arm left: 40-44 x, 52-64 y
    draw.rectangle([40, 52, 43, 63], fill=black)
    # Left arm back: 44-48 x, 52-64 y
    draw.rectangle([44, 52, 47, 63], fill=black)
    
    # === RIGHT LEG ===
    # Right leg top: 4-8 x, 16-20 y
    draw.rectangle([4, 16, 7, 19], fill=black)
    # Right leg bottom: 8-12 x, 16-20 y
    draw.rectangle([8, 16, 11, 19], fill=black)
    # Right leg right: 0-4 x, 20-32 y
    draw.rectangle([0, 20, 3, 31], fill=black)
    # Right leg front: 4-8 x, 20-32 y
    draw.rectangle([4, 20, 7, 31], fill=black)
    # Right leg left: 8-12 x, 20-32 y
    draw.rectangle([8, 20, 11, 31], fill=black)
    # Right leg back: 12-16 x, 20-32 y
    draw.rectangle([12, 20, 15, 31], fill=black)
    
    # === LEFT LEG (1.8+ format, bottom half) ===
    # Left leg top: 20-24 x, 48-52 y
    draw.rectangle([20, 48, 23, 51], fill=black)
    # Left leg bottom: 24-28 x, 48-52 y
    draw.rectangle([24, 48, 27, 51], fill=black)
    # Left leg right: 16-20 x, 52-64 y
    draw.rectangle([16, 52, 19, 63], fill=black)
    # Left leg front: 20-24 x, 52-64 y
    draw.rectangle([20, 52, 23, 63], fill=black)
    # Left leg left: 24-28 x, 52-64 y
    draw.rectangle([24, 52, 27, 63], fill=black)
    # Left leg back: 28-32 x, 52-64 y
    draw.rectangle([28, 52, 31, 63], fill=black)
    
    # === OVERLAY LAYERS (jacket, sleeves, pants) - fill with black ===
    # Body overlay: 20-28 x, 36-48 y area
    draw.rectangle([20, 36, 27, 47], fill=black)
    draw.rectangle([16, 36, 19, 47], fill=black)
    draw.rectangle([28, 36, 31, 47], fill=black)
    draw.rectangle([32, 36, 39, 47], fill=black)
    draw.rectangle([20, 32, 27, 35], fill=black)
    draw.rectangle([28, 32, 35, 35], fill=black)
    
    # Right arm overlay
    draw.rectangle([44, 32, 47, 35], fill=black)
    draw.rectangle([48, 32, 51, 35], fill=black)
    draw.rectangle([40, 36, 43, 47], fill=black)
    draw.rectangle([44, 36, 47, 47], fill=black)
    draw.rectangle([48, 36, 51, 47], fill=black)
    draw.rectangle([52, 36, 55, 47], fill=black)
    
    # Left arm overlay
    draw.rectangle([52, 48, 55, 51], fill=black)
    draw.rectangle([56, 48, 59, 51], fill=black)
    draw.rectangle([48, 52, 51, 63], fill=black)
    draw.rectangle([52, 52, 55, 63], fill=black)
    draw.rectangle([56, 52, 59, 63], fill=black)
    draw.rectangle([60, 52, 63, 63], fill=black)
    
    # Right leg overlay
    draw.rectangle([4, 32, 7, 35], fill=black)
    draw.rectangle([8, 32, 11, 35], fill=black)
    draw.rectangle([0, 36, 3, 47], fill=black)
    draw.rectangle([4, 36, 7, 47], fill=black)
    draw.rectangle([8, 36, 11, 47], fill=black)
    draw.rectangle([12, 36, 15, 47], fill=black)
    
    # Left leg overlay
    draw.rectangle([4, 48, 7, 51], fill=black)
    draw.rectangle([8, 48, 11, 51], fill=black)
    draw.rectangle([0, 52, 3, 63], fill=black)
    draw.rectangle([4, 52, 7, 63], fill=black)
    draw.rectangle([8, 52, 11, 63], fill=black)
    draw.rectangle([12, 52, 15, 63], fill=black)
    
    # === ADD GLOWING WHITE EYES (THE ICONIC HEROBRINE EYES) ===
    # Eyes are on the head front face (8-16 x, 8-16 y)
    # The face is 8 pixels wide, eyes are typically at y=11-12 (row 3-4 of face)
    # Herobrine has pure white, glowing eyes - 2 pixels each
    
    # Left eye (2 pixels wide) - positioned like Steve's eyes but WHITE
    # x=9-10, y=11-12
    draw.rectangle([9, 11, 10, 12], fill=white_glow)
    
    # Right eye (2 pixels wide)
    # x=13-14, y=11-12
    draw.rectangle([13, 11, 14, 12], fill=white_glow)
    
    # Make eyes extra bright in center for glow effect
    img.putpixel((9, 11), white_bright)
    img.putpixel((10, 11), white_bright)
    img.putpixel((13, 11), white_bright)
    img.putpixel((14, 11), white_bright)
    
    # Also add eyes to the hat/overlay layer for extra glow effect
    # Hat front is at 40-48 x, 8-16 y
    draw.rectangle([41, 11, 42, 12], fill=white_glow)
    draw.rectangle([45, 11, 46, 12], fill=white_glow)
    
    # Save the image
    output_path = "src/main/resources/assets/leo_enchants/textures/entity/herobrine.png"
    img.save(output_path, "PNG")
    print(f"Herobrine skin saved to: {output_path}")
    print("Skin features:")
    print("  - Pure black body (RGB 0,0,0)")
    print("  - Glowing white eyes (RGB 255,255,255) - the iconic Herobrine look")
    print("  - Full 64x64 player skin format with all layers")
    
    return img


if __name__ == "__main__":
    create_herobrine_skin()


