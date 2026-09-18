Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile("C:\Users\krish\Desktop\tsundoku-main\app\src\main\res\drawable\ic_launcher_foreground.jpg") 
$size = 1152
$bmp = New-Object System.Drawing.Bitmap($size, $size)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.Clear([System.Drawing.Color]::Transparent)

# Fit image to fill the 1152x1152 canvas exactly
$g.DrawImage($src, 0, 0, $size, $size)

$g.Dispose()
$src.Dispose()
$bmp.Save("C:\Users\krish\Desktop\tsundoku-main\app\src\main\res\drawable-nodpi\wammy_logo.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "Created 1152x1152 high-res wammy_logo in nodpi!"
