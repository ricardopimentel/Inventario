Add-Type -AssemblyName System.Drawing
$imgPath = 'C:\Users\2306214\.gemini\antigravity\brain\be774406-d772-4fd4-8593-711e0a74f7c4\app_icon_cgti_1774905955064.png'
$img = [System.Drawing.Image]::FromFile($imgPath)
$sizes = @{
    'mdpi'   = 48
    'hdpi'   = 72
    'xhdpi'  = 96
    'xxhdpi' = 144
    'xxxhdpi'= 192
}

foreach ($k in $sizes.Keys) {
    $s = $sizes[$k]
    $bmp = New-Object System.Drawing.Bitmap($s, $s)
    $graph = [System.Drawing.Graphics]::FromImage($bmp)
    
    # High-quality interpolation
    $graph.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graph.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graph.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graph.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    $graph.DrawImage($img, 0, 0, $s, $s)
    
    $path = "d:\Documentos\Android Projects\Inventario\app\src\main\res\mipmap-$k"
    if (-not (Test-Path $path)) { 
        New-Item -ItemType Directory -Path $path | Out-Null
    }
    
    $outFile = "$path\icone.png"
    $bmp.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
    
    $graph.Dispose()
    $bmp.Dispose()
}

$img.Dispose()
Write-Host "Icons generated successfully!"
