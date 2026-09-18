Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile("C:\Users\krish\Desktop\tsundoku-main\app\src\main\res\drawable\wammy_logo_old.png") 
$size = 432
$bmp = New-Object System.Drawing.Bitmap($size, $size)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.Clear([System.Drawing.Color]::Transparent)

$ratio = [math]::Min($size / $src.Width, $size / $src.Height)
$w = [int]($src.Width * $ratio)
$h = [int]($src.Height * $ratio)
$x = [int](($size - $w) / 2)
$y = [int](($size - $h) / 2)

$g.DrawImage($src, $x, $y, $w, $h)
$g.Dispose()
$src.Dispose()
$bmp.Save("C:\Users\krish\Desktop\tsundoku-main\app\src\main\res\drawable\wammy_logo.png", [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "Logo fixed and centered properly!"
