Add-Type -TypeDefinition @"
using System.Drawing;
using System.Drawing.Drawing2D;
public class IconResizer2 {
    public static void Resize(string imgPath, string outPath, int size, int margin) {
        using (var img = Image.FromFile(imgPath)) {
            using (var bmp = new Bitmap(size, size)) {
                using (var g = Graphics.FromImage(bmp)) {
                    g.Clear(Color.Transparent);
                    g.InterpolationMode = InterpolationMode.HighQualityBicubic;
                    g.SmoothingMode = SmoothingMode.HighQuality;
                    g.PixelOffsetMode = PixelOffsetMode.HighQuality;
                    Rectangle dest = new Rectangle(0, 0, size, size);
                    Rectangle src = new Rectangle(margin, margin, img.Width - 2*margin, img.Height - 2*margin);
                    g.DrawImage(img, dest, src, GraphicsUnit.Pixel);
                }
                bmp.Save(outPath, System.Drawing.Imaging.ImageFormat.Png);
            }
        }
    }
}
"@ -ReferencedAssemblies System.Drawing

$imgPath = 'C:\Users\2306214\.gemini\antigravity\brain\be774406-d772-4fd4-8593-711e0a74f7c4\app_icon_cgti_1774905955064.png'
# Reduce crop from 20% to 5% to just remove the absolute outermost whitespace.
$margin = [int](1024 * 0.05)

$sizes = @{'mdpi'=48; 'hdpi'=72; 'xhdpi'=96; 'xxhdpi'=144; 'xxxhdpi'=192}

foreach ($k in $sizes.Keys) {
    $s = $sizes[$k]
    $path = "d:\Documentos\Android Projects\Inventario\app\src\main\res\mipmap-$k"
    if (-not (Test-Path $path)) { New-Item -ItemType Directory -Path $path | Out-Null }
    [IconResizer2]::Resize($imgPath, "$path\icone.png", $s, $margin)
}

Write-Host "Icons fixed!"
