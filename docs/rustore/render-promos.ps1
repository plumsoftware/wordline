param([string]$Backdrop = "$PSScriptRoot/promo/source/backdrop.png")
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$screenRoot = Join-Path $projectRoot 'docs/spec/soedini-slova-tz-assets-v1.1-complete/soedini-slova-tz-v1.1/design/screens'
$outDir = Join-Path $PSScriptRoot 'promo/upload'
$masterDir = Join-Path $PSScriptRoot 'promo/masters'
New-Item -ItemType Directory -Force -Path $outDir,$masterDir | Out-Null
$items = @(
    @{Id='01-gameplay'; Screen='S05-word-selection'; Lines=@('Соединяй буквы,','находи слова'); Accent='буквы'},
    @{Id='02-home'; Screen='S02-home'; Lines=@('Открывай мир','слово за словом'); Accent='мир'},
    @{Id='03-collection'; Screen='S19-collection'; Lines=@('Твоя коллекция','красивых мест'); Accent='коллекция'},
    @{Id='04-map'; Screen='S03-map'; Lines=@('Шаг за шагом —','к новым открытиям'); Accent='открытиям'},
    @{Id='05-daily'; Screen='S15-daily-crossword'; Lines=@('Новый кроссворд','каждый день'); Accent='кроссворд'},
    @{Id='06-gift'; Screen='S16-daily-gift'; Lines=@('Подарки ждут','каждый день'); Accent='Подарки'}
)
function Round-Path([single]$x,[single]$y,[single]$w,[single]$h,[single]$r) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r*2
    $path.AddArc($x,$y,$d,$d,180,90)
    $path.AddArc(($x+$w-$d),$y,$d,$d,270,90)
    $path.AddArc(($x+$w-$d),($y+$h-$d),$d,$d,0,90)
    $path.AddArc($x,($y+$h-$d),$d,$d,90,90)
    $path.CloseFigure()
    return ,$path
}
$background = [Drawing.Image]::FromFile([IO.Path]::GetFullPath($Backdrop))
$plaque = [Drawing.Image]::FromFile((Join-Path $PSScriptRoot 'promo/source/title-plaque.png'))
$font = New-Object Drawing.Font('Segoe UI',84,[Drawing.FontStyle]::Bold,[Drawing.GraphicsUnit]::Pixel)
$ink = New-Object Drawing.SolidBrush([Drawing.ColorTranslator]::FromHtml('#071738'))
$accentInk = New-Object Drawing.SolidBrush([Drawing.ColorTranslator]::FromHtml('#078452'))
$body = New-Object Drawing.SolidBrush([Drawing.ColorTranslator]::FromHtml('#183F43'))
$edge = New-Object Drawing.Pen([Drawing.ColorTranslator]::FromHtml('#96BEB0'),3)
$speaker = New-Object Drawing.SolidBrush([Drawing.ColorTranslator]::FromHtml('#0C252B'))
$format = New-Object Drawing.StringFormat
$format.Alignment = [Drawing.StringAlignment]::Center
$format.LineAlignment = [Drawing.StringAlignment]::Center
$format.FormatFlags = [Drawing.StringFormatFlags]::NoWrap
$codec = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -EQ 'image/jpeg'
$quality = New-Object Drawing.Imaging.EncoderParameters(1)
$quality.Param[0] = New-Object Drawing.Imaging.EncoderParameter([Drawing.Imaging.Encoder]::Quality,[long]94)
$phoneRect = [Drawing.RectangleF]::new(166,342,748,1550)
$screenRect = [Drawing.RectangleF]::new(190,370,700,1494)
$records = @()
try {
    foreach($item in $items) {
        $bitmap = New-Object Drawing.Bitmap(1080,1920,[Drawing.Imaging.PixelFormat]::Format24bppRgb)
        $g = [Drawing.Graphics]::FromImage($bitmap)
        $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.TextRenderingHint = [Drawing.Text.TextRenderingHint]::AntiAliasGridFit
        $g.DrawImage($background,[Drawing.Rectangle]::new(0,0,1080,1920))
        # Three-slice plaque: taller cream centre, small leaves at the edges, room for both full lines.
        $sourceEdge=[single]($plaque.Width*.15)
        $g.DrawImage($plaque,[Drawing.RectangleF]::new(22,0,92,356),[Drawing.RectangleF]::new(0,0,$sourceEdge,$plaque.Height),[Drawing.GraphicsUnit]::Pixel)
        $g.DrawImage($plaque,[Drawing.RectangleF]::new(114,0,852,356),[Drawing.RectangleF]::new($sourceEdge,0,($plaque.Width-2*$sourceEdge),$plaque.Height),[Drawing.GraphicsUnit]::Pixel)
        $g.DrawImage($plaque,[Drawing.RectangleF]::new(966,0,92,356),[Drawing.RectangleF]::new(($plaque.Width-$sourceEdge),0,$sourceEdge,$plaque.Height),[Drawing.GraphicsUnit]::Pixel)
        # The only promotional text: exactly two lines in the same fixed boxes on all six exports.
        for($line=0;$line -lt 2;$line++) {
            $box = [Drawing.RectangleF]::new(84,(94+$line*94),912,100)
            if($g.MeasureString($item.Lines[$line],$font).Width -gt 910) {throw "Headline exceeds its fixed box: $($item.Id)"}
            $g.DrawString($item.Lines[$line],$font,$ink,$box,$format)
            $accentAt=$item.Lines[$line].IndexOf($item.Accent,[StringComparison]::OrdinalIgnoreCase)
            if($accentAt -ge 0) {
                $accentFormat=$format.Clone()
                $accentFormat.SetMeasurableCharacterRanges([Drawing.CharacterRange[]]@([Drawing.CharacterRange]::new($accentAt,$item.Accent.Length)))
                $regions=$g.MeasureCharacterRanges($item.Lines[$line],$font,$box,$accentFormat)
                $state=$g.Save();$g.SetClip($regions[0],[Drawing.Drawing2D.CombineMode]::Replace)
                $g.DrawString($item.Lines[$line],$font,$accentInk,$box,$format)
                $g.Restore($state);$regions | ForEach-Object {$_.Dispose()};$accentFormat.Dispose()
            }
        }
        for($layer=15;$layer -ge 1;$layer--) {
            $shadow = New-Object Drawing.SolidBrush([Drawing.Color]::FromArgb(3,2,31,36))
            $path = Round-Path (166-$layer) (354-$layer) (748+2*$layer) (1550+2*$layer) (80+$layer)
            $g.FillPath($shadow,$path);$path.Dispose();$shadow.Dispose()
        }
        $path = Round-Path 166 342 748 1550 80
        $g.FillPath($body,$path);$g.DrawPath($edge,$path);$path.Dispose()
        $g.FillRectangle($body,159,588,7,140)
        $g.FillRectangle($body,914,672,7,170)
        $path = Round-Path 473 353 134 6 3
        $g.FillPath($speaker,$path);$path.Dispose()
        $screenPath = Round-Path 190 370 700 1494 59
        $saved = $g.Save();$g.SetClip($screenPath)
        $g.DrawImage($background,$screenRect)
        # Same exact screenshot rectangle in every poster; the prepared screens have one aspect ratio.
        $source = [Drawing.Image]::FromFile((Join-Path $PSScriptRoot ('promo/screens/'+$item.Id+'.png')))
        try {
            $g.DrawImage($source,$screenRect)
        } finally {$source.Dispose()}
        $g.Restore($saved);$screenPath.Dispose()
        $master = Join-Path $masterDir ($item.Id+'.png')
        $upload = Join-Path $outDir ($item.Id+'.jpg')
        $bitmap.Save($master,[Drawing.Imaging.ImageFormat]::Png)
        $bitmap.Save($upload,$codec,$quality)
        $g.Dispose();$bitmap.Dispose()
        $file = Get-Item -LiteralPath $upload
        if($file.Length -ge 3000000) {throw "Upload is too large: $upload"}
        $records += @{file=('promo/upload/'+$item.Id+'.jpg');screen=('promo/screens/'+$item.Id+'.png');reference=($item.Screen+'.png');headline=$item.Lines;accent=$item.Accent;width=1080;height=1920;bytes=$file.Length}
    }
} finally {
    $background.Dispose();$plaque.Dispose();$font.Dispose();$ink.Dispose();$accentInk.Dispose();$body.Dispose();$edge.Dispose();$speaker.Dispose();$format.Dispose();$quality.Dispose()
}
$manifest = @{version=2;canvas=@{width=1080;height=1920};headline=@{x=84;y=94;width=912;height=194;lines=2;font='Segoe UI Bold';fontSize=84;accentColor='#078452';plaque=@{x=22;y=0;width=1036;height=356;edgeWidth=92;fit='three-slice'}};phone=@{x=166;y=342;width=748;height=1550};screenViewport=@{x=190;y=370;width=700;height=1494;fit='fill'};files=$records}
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'promo-manifest.json') -Encoding UTF8
$records | ForEach-Object { Write-Output "$($_.file): $($_.width)x$($_.height), $($_.bytes) bytes" }
$preview = New-Object Drawing.Bitmap(972,1152)
$previewGraphics = [Drawing.Graphics]::FromImage($preview)
$previewGraphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
for($i=0;$i -lt $items.Count;$i++) {
    $image = [Drawing.Image]::FromFile((Join-Path $outDir ($items[$i].Id+'.jpg')))
    $previewGraphics.DrawImage($image,[Drawing.Rectangle]::new((($i%3)*324),([Math]::Floor($i/3)*576),324,576))
    $image.Dispose()
}
$preview.Save((Join-Path $PSScriptRoot 'promo/preview.jpg'),[Drawing.Imaging.ImageFormat]::Jpeg)
$previewGraphics.Dispose();$preview.Dispose()
