"""
Generate textures for the Space Travel item and portal for the Leo Enchants mod.
Creates:
1. 16x16 pixel item texture with a cosmic/dimensional appearance
2. 64x64 portal surface texture with swirling space/dimension preview effect
"""

from PIL import Image, ImageDraw, ImageFilter
import os
import math
import random


def generate_space_travel_item_texture():
    """
    Generate the space-travel item texture.
    A cosmic orb with swirling dimensional energies - representing all three dimensions.
    """
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    
    # Color palette - cosmic theme with dimension accents
    deep_space = (10, 5, 25, 255)           # Deep space purple
    nether_red = (180, 50, 30, 255)         # Nether accent
    end_purple = (160, 80, 200, 255)        # End accent
    overworld_blue = (60, 140, 220, 255)    # Overworld accent
    overworld_green = (80, 180, 100, 255)   # Overworld grass
    star_white = (255, 255, 255, 255)       # Stars
    star_dim = (200, 200, 255, 200)         # Dim stars
    portal_glow = (150, 100, 255, 230)      # Portal energy
    obsidian_dark = (20, 15, 30, 255)       # Obsidian base
    dirt_brown = (120, 85, 60, 255)         # Dirt element
    end_stone = (220, 220, 170, 255)        # End stone element
    
    # Draw the base orb shape (cosmic sphere)
    center_x, center_y = 8, 8
    radius = 6
    
    for y in range(16):
        for x in range(16):
            dx = x - center_x + 0.5
            dy = y - center_y + 0.5
            dist = math.sqrt(dx * dx + dy * dy)
            
            if dist <= radius:
                # Inside the orb
                # Create depth effect based on distance from center
                depth = 1 - (dist / radius)
                
                # Swirling pattern - mix of dimension colors
                angle = math.atan2(dy, dx)
                swirl = math.sin(angle * 3 + dist * 0.5) * 0.5 + 0.5
                
                # Mix colors based on swirl and position
                if swirl < 0.33:
                    # Overworld zone (blue-green)
                    r = int(overworld_blue[0] * (1 - swirl * 3) + overworld_green[0] * (swirl * 3))
                    g = int(overworld_blue[1] * (1 - swirl * 3) + overworld_green[1] * (swirl * 3))
                    b = int(overworld_blue[2] * (1 - swirl * 3) + overworld_green[2] * (swirl * 3))
                elif swirl < 0.66:
                    # Nether zone (red-orange)
                    blend = (swirl - 0.33) * 3
                    r = int(overworld_green[0] * (1 - blend) + nether_red[0] * blend)
                    g = int(overworld_green[1] * (1 - blend) + nether_red[1] * blend)
                    b = int(overworld_green[2] * (1 - blend) + nether_red[2] * blend)
                else:
                    # End zone (purple)
                    blend = (swirl - 0.66) * 3
                    r = int(nether_red[0] * (1 - blend) + end_purple[0] * blend)
                    g = int(nether_red[1] * (1 - blend) + end_purple[1] * blend)
                    b = int(nether_red[2] * (1 - blend) + end_purple[2] * blend)
                
                # Apply depth shading
                r = int(r * (0.4 + 0.6 * depth))
                g = int(g * (0.4 + 0.6 * depth))
                b = int(b * (0.4 + 0.6 * depth))
                
                img.putpixel((x, y), (r, g, b, 255))
            elif dist <= radius + 0.7:
                # Edge glow
                edge_fade = 1 - ((dist - radius) / 0.7)
                img.putpixel((x, y), (portal_glow[0], portal_glow[1], portal_glow[2], int(180 * edge_fade)))
    
    # Add ingredient elements (dirt, obsidian, end_stone fragments)
    # Dirt fragment (bottom left)
    img.putpixel((4, 11), dirt_brown)
    img.putpixel((3, 12), (dirt_brown[0] - 20, dirt_brown[1] - 20, dirt_brown[2] - 10, 255))
    
    # Obsidian fragment (bottom right)
    img.putpixel((11, 11), obsidian_dark)
    img.putpixel((12, 12), (obsidian_dark[0] + 15, obsidian_dark[1] + 10, obsidian_dark[2] + 20, 255))
    
    # End stone fragment (top)
    img.putpixel((7, 3), end_stone)
    img.putpixel((8, 3), (end_stone[0] - 10, end_stone[1] - 10, end_stone[2] - 20, 255))
    
    # Add stars/sparkles
    star_positions = [(5, 5), (10, 6), (6, 10), (9, 9), (7, 7)]
    for i, (sx, sy) in enumerate(star_positions):
        if i == 0:
            img.putpixel((sx, sy), star_white)
        else:
            img.putpixel((sx, sy), star_dim)
    
    # Add central bright spot
    img.putpixel((8, 7), (255, 220, 255, 255))
    img.putpixel((7, 8), (220, 200, 255, 230))
    
    # Add highlight
    img.putpixel((5, 5), star_white)
    img.putpixel((6, 5), (255, 255, 255, 200))
    img.putpixel((5, 6), (255, 255, 255, 180))
    
    return img


def generate_space_portal_texture():
    """
    Generate the space portal surface texture.
    A swirling dimensional rift that previews the destination.
    64x64 pixels for more detail.
    """
    size = 64
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    # Color palettes for each dimension
    overworld_colors = [
        (100, 180, 255),   # Sky blue
        (80, 200, 120),    # Grass green
        (240, 240, 240),   # Cloud white
        (60, 140, 200),    # Water blue
    ]
    
    nether_colors = [
        (200, 60, 30),     # Lava orange
        (140, 30, 20),     # Netherrack red
        (255, 180, 60),    # Fire yellow
        (80, 20, 20),      # Dark crimson
    ]
    
    end_colors = [
        (180, 120, 255),   # End purple
        (220, 200, 150),   # End stone
        (20, 10, 30),      # Void black
        (255, 255, 200),   # End crystal glow
    ]
    
    center_x, center_y = size // 2, size // 2
    
    # Create swirling vortex pattern
    for y in range(size):
        for x in range(size):
            dx = x - center_x
            dy = y - center_y
            dist = math.sqrt(dx * dx + dy * dy)
            max_dist = size // 2
            
            if dist > max_dist:
                continue
            
            # Normalize distance
            norm_dist = dist / max_dist
            
            # Calculate angle with swirl
            angle = math.atan2(dy, dx)
            swirl_angle = angle + norm_dist * 4.0  # Swirl intensity
            
            # Use angle to select color from palette (cycle through all dimensions)
            color_angle = (swirl_angle + norm_dist * 2) % (2 * math.pi)
            section = color_angle / (2 * math.pi)
            
            # Blend between dimension palettes based on angle
            if section < 0.33:
                # Overworld section
                blend = section * 3
                idx = int(blend * (len(overworld_colors) - 1))
                next_idx = min(idx + 1, len(overworld_colors) - 1)
                color1 = overworld_colors[idx]
                color2 = overworld_colors[next_idx]
                t = (blend * (len(overworld_colors) - 1)) % 1
            elif section < 0.66:
                # Nether section
                blend = (section - 0.33) * 3
                idx = int(blend * (len(nether_colors) - 1))
                next_idx = min(idx + 1, len(nether_colors) - 1)
                color1 = nether_colors[idx]
                color2 = nether_colors[next_idx]
                t = (blend * (len(nether_colors) - 1)) % 1
            else:
                # End section
                blend = (section - 0.66) * 3
                idx = int(blend * (len(end_colors) - 1))
                next_idx = min(idx + 1, len(end_colors) - 1)
                color1 = end_colors[idx]
                color2 = end_colors[next_idx]
                t = (blend * (len(end_colors) - 1)) % 1
            
            # Lerp between colors
            r = int(color1[0] * (1 - t) + color2[0] * t)
            g = int(color1[1] * (1 - t) + color2[1] * t)
            b = int(color1[2] * (1 - t) + color2[2] * t)
            
            # Apply depth fade (darker at edges)
            brightness = 1.0 - norm_dist * 0.6
            r = int(r * brightness)
            g = int(g * brightness)
            b = int(b * brightness)
            
            # Alpha fade at edges
            alpha = int(255 * (1 - norm_dist * 0.3))
            
            img.putpixel((x, y), (r, g, b, alpha))
    
    # Add sparkle/star effects
    random.seed(42)  # Consistent pattern
    for _ in range(30):
        sx = random.randint(5, size - 5)
        sy = random.randint(5, size - 5)
        dx = sx - center_x
        dy = sy - center_y
        dist = math.sqrt(dx * dx + dy * dy)
        if dist < size // 2 - 5:
            brightness = random.randint(200, 255)
            img.putpixel((sx, sy), (brightness, brightness, brightness, 255))
    
    # Add central bright core
    for y in range(center_y - 3, center_y + 4):
        for x in range(center_x - 3, center_x + 4):
            dx = x - center_x
            dy = y - center_y
            dist = math.sqrt(dx * dx + dy * dy)
            if dist < 3:
                intensity = int(255 * (1 - dist / 3))
                current = img.getpixel((x, y))
                new_r = min(255, current[0] + intensity)
                new_g = min(255, current[1] + intensity)
                new_b = min(255, current[2] + intensity)
                img.putpixel((x, y), (new_r, new_g, new_b, 255))
    
    return img


def main():
    # Get project paths
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    
    # Item texture output path
    item_output_dir = os.path.join(
        project_root,
        'src', 'main', 'resources', 'assets', 'leo_enchants', 'textures', 'item'
    )
    
    # Entity texture output path
    entity_output_dir = os.path.join(
        project_root,
        'src', 'main', 'resources', 'assets', 'leo_enchants', 'textures', 'entity'
    )
    
    # Create directories if they don't exist
    os.makedirs(item_output_dir, exist_ok=True)
    os.makedirs(entity_output_dir, exist_ok=True)
    
    # Generate and save item texture
    item_texture = generate_space_travel_item_texture()
    item_path = os.path.join(item_output_dir, 'space_travel.png')
    item_texture.save(item_path, 'PNG')
    print(f"Generated space travel item texture: {item_path}")
    print(f"  Size: {item_texture.size}, Mode: {item_texture.mode}")
    
    # Generate and save portal texture
    portal_texture = generate_space_portal_texture()
    portal_path = os.path.join(entity_output_dir, 'space_portal.png')
    portal_texture.save(portal_path, 'PNG')
    print(f"Generated space portal texture: {portal_path}")
    print(f"  Size: {portal_texture.size}, Mode: {portal_texture.mode}")
    
    print("\nAll textures generated successfully!")


if __name__ == '__main__':
    main()


