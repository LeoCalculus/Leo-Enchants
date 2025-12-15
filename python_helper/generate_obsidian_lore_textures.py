"""
Generates textures for the Obsidian Lore item:
1. Build mode - Enhanced obsidian with a mystical purple glow
2. Attack mode - Obsidian stripe/spike shape (like a fry/shard)

Requirements: pip install Pillow

Output:
- src/main/resources/assets/leo_enchants/textures/item/obsidian_lore.png (build mode)
- src/main/resources/assets/leo_enchants/textures/item/obsidian_lore_attack.png (attack mode)
"""

from PIL import Image, ImageDraw, ImageFilter
import random

# Obsidian color palette (from actual Minecraft obsidian)
OBSIDIAN_DARK = (15, 10, 24)      # Very dark purple-black
OBSIDIAN_MID = (20, 18, 30)       # Mid-tone
OBSIDIAN_HIGHLIGHT = (45, 35, 60) # Purple highlight
OBSIDIAN_SHINE = (80, 60, 100)    # Light reflection
PURPLE_GLOW = (120, 40, 180)      # Mystical purple glow
PURPLE_BRIGHT = (180, 80, 220)    # Bright purple accent

def create_obsidian_lore_build_texture():
    """Create the build mode texture - enhanced obsidian block with purple glow"""
    width, height = 16, 16
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    
    # Base obsidian texture pattern
    for y in range(height):
        for x in range(width):
            # Create noisy obsidian pattern
            noise = random.random()
            if noise < 0.5:
                color = OBSIDIAN_DARK
            elif noise < 0.8:
                color = OBSIDIAN_MID
            elif noise < 0.95:
                color = OBSIDIAN_HIGHLIGHT
            else:
                color = OBSIDIAN_SHINE
            
            img.putpixel((x, y), (*color, 255))
    
    # Add purple mystical glow edges
    draw = ImageDraw.Draw(img)
    
    # Top edge glow
    for x in range(width):
        if random.random() > 0.3:
            img.putpixel((x, 0), (*PURPLE_GLOW, 200))
        if random.random() > 0.5:
            img.putpixel((x, 1), (*PURPLE_GLOW, 100))
    
    # Bottom edge glow
    for x in range(width):
        if random.random() > 0.3:
            img.putpixel((x, 15), (*PURPLE_GLOW, 200))
        if random.random() > 0.5:
            img.putpixel((x, 14), (*PURPLE_GLOW, 100))
    
    # Left edge glow
    for y in range(height):
        if random.random() > 0.3:
            img.putpixel((0, y), (*PURPLE_GLOW, 200))
        if random.random() > 0.5:
            img.putpixel((1, y), (*PURPLE_GLOW, 100))
    
    # Right edge glow
    for y in range(height):
        if random.random() > 0.3:
            img.putpixel((15, y), (*PURPLE_GLOW, 200))
        if random.random() > 0.5:
            img.putpixel((14, y), (*PURPLE_GLOW, 100))
    
    # Add mystical rune symbol in center (simplified eye/diamond shape)
    # Center diamond shape
    center_points = [
        (8, 4), (7, 5), (8, 5), (9, 5),
        (6, 6), (7, 6), (8, 6), (9, 6), (10, 6),
        (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7),
        (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8),
        (6, 9), (7, 9), (8, 9), (9, 9), (10, 9),
        (7, 10), (8, 10), (9, 10),
        (8, 11),
    ]
    
    for x, y in center_points:
        if 0 <= x < 16 and 0 <= y < 16:
            # Inner glow
            dist_from_center = abs(x - 8) + abs(y - 7.5)
            if dist_from_center < 2:
                img.putpixel((x, y), (*PURPLE_BRIGHT, 255))
            else:
                img.putpixel((x, y), (*PURPLE_GLOW, 220))
    
    # Save
    output_path = "src/main/resources/assets/leo_enchants/textures/item/obsidian_lore.png"
    img.save(output_path, "PNG")
    print(f"Build mode texture saved to: {output_path}")
    return img


def create_obsidian_lore_attack_texture():
    """Create the attack mode texture - obsidian stripe/spike shape (like a fry/shard)"""
    width, height = 16, 16
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    
    # Create a diagonal spike/shard shape (like a french fry or javelin)
    # The spike goes from bottom-left to top-right diagonally
    
    # Define the spike shape - narrow and elongated
    spike_pixels = []
    
    # Main spike body (diagonal from bottom-left to top-right)
    for i in range(14):
        y = 15 - i  # Start from bottom
        x = 1 + i   # Move right
        
        # Width varies - thicker in middle, thinner at ends
        if i < 3 or i > 11:
            # Thin ends
            spike_pixels.append((x, y))
        elif i < 5 or i > 9:
            # Medium thickness
            spike_pixels.append((x, y))
            spike_pixels.append((x, y-1))
        else:
            # Thick middle
            spike_pixels.append((x, y))
            spike_pixels.append((x, y-1))
            spike_pixels.append((x+1, y))
    
    # Add tip (sharp point at top-right)
    spike_pixels.extend([(14, 2), (15, 1)])
    
    # Add base (bottom-left)
    spike_pixels.extend([(0, 15), (1, 14), (0, 14)])
    
    # Fill in the spike with obsidian texture
    for x, y in spike_pixels:
        if 0 <= x < 16 and 0 <= y < 16:
            # Create noisy obsidian pattern
            noise = random.random()
            if noise < 0.4:
                color = OBSIDIAN_DARK
            elif noise < 0.7:
                color = OBSIDIAN_MID
            elif noise < 0.9:
                color = OBSIDIAN_HIGHLIGHT
            else:
                color = OBSIDIAN_SHINE
            
            img.putpixel((x, y), (*color, 255))
    
    # Add purple glow along edges
    edge_glow_pixels = [
        # Top edge of spike
        (2, 13), (3, 12), (4, 11), (5, 10), (6, 9), (7, 8), (8, 7), 
        (9, 6), (10, 5), (11, 4), (12, 3), (13, 2), (14, 1),
        # Tip glow
        (15, 0), (15, 1), (14, 2),
    ]
    
    for x, y in edge_glow_pixels:
        if 0 <= x < 16 and 0 <= y < 16:
            current = img.getpixel((x, y))
            if current[3] == 0:  # Only add glow to transparent pixels
                img.putpixel((x, y), (*PURPLE_GLOW, 150))
    
    # Add bright tip
    img.putpixel((15, 1), (*PURPLE_BRIGHT, 255))
    img.putpixel((14, 2), (*PURPLE_GLOW, 230))
    
    # Add some internal purple streaks
    streak_pixels = [(5, 11), (7, 9), (9, 7), (11, 5), (13, 3)]
    for x, y in streak_pixels:
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), (*PURPLE_GLOW, 200))
    
    # Save
    output_path = "src/main/resources/assets/leo_enchants/textures/item/obsidian_lore_attack.png"
    img.save(output_path, "PNG")
    print(f"Attack mode texture saved to: {output_path}")
    return img


if __name__ == "__main__":
    print("Generating Obsidian Lore textures...")
    print("-" * 40)
    
    create_obsidian_lore_build_texture()
    print("  - Build mode: Enhanced obsidian with mystical purple glow")
    print()
    
    create_obsidian_lore_attack_texture()
    print("  - Attack mode: Obsidian spike/shard shape")
    print()
    
    print("-" * 40)
    print("Done! Textures generated successfully.")
    print("\nFeatures:")
    print("  Build Mode: Obsidian block with purple mystical aura and rune")
    print("  Attack Mode: Sharp obsidian stripe/spike like a javelin")

