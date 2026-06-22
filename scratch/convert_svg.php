<?php
// ============================================
// SVG to Android VectorDrawable Converter
// ============================================

$inputFile = __DIR__ . '/../web/user/onboarding.php';
$outputDir = __DIR__ . '/../app/src/main/res/drawable';

if (!file_exists($inputFile)) {
    die("Input file onboarding.php not found at: $inputFile\n");
}

$content = file_get_contents($inputFile);

// Regex to extract SVGs
preg_match_all('/<svg[^>]*illus-svg[^>]*>(.*?)<\/svg>/is', $content, $matches);

if (empty($matches[0])) {
    die("No SVGs found in onboarding.php\n");
}

echo "Found " . count($matches[0]) . " SVGs.\n";

$slideNumber = 1;
foreach ($matches[0] as $svgMarkup) {
    // Parse SVG ViewBox
    $viewBox = '0 0 300 260';
    if (preg_match('/viewBox="([^"]+)"/', $svgMarkup, $vbMatch)) {
        $viewBox = $vbMatch[1];
    }
    $parts = explode(' ', $viewBox);
    $width = isset($parts[2]) ? $parts[2] : 300;
    $height = isset($parts[3]) ? $parts[3] : 260;

    $xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
    $xml .= "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n";
    $xml .= "    android:width=\"{$width}dp\"\n";
    $xml .= "    android:height=\"{$height}dp\"\n";
    $xml .= "    android:viewportWidth=\"{$width}\"\n";
    $xml .= "    android:viewportHeight=\"{$height}\">\n";

    // Load XML document to parse elements
    // Pre-process XML slightly to avoid namespace issues or tag errors
    $cleanedMarkup = preg_replace('/xmlns="[^"]+"/', '', $svgMarkup);
    $dom = new DOMDocument();
    @$dom->loadXML($cleanedMarkup);

    $xpath = new DOMXPath($dom);
    // Find all children inside svg
    $elements = $xpath->query('//*[not(self::svg)]');

    foreach ($elements as $el) {
        $nodeName = $el->nodeName;
        if ($nodeName == 'ellipse' || $nodeName == 'rect' || $nodeName == 'circle' || $nodeName == 'line' || $nodeName == 'path') {
            $pathData = '';
            $strokeWidth = $el->getAttribute('stroke-width');
            $strokeColorAttr = $el->getAttribute('stroke');
            $fillColorAttr = $el->getAttribute('fill');
            $opacityAttr = $el->getAttribute('opacity');

            // Set default fill/stroke values
            $fillColor = '#000000';
            $fillAlpha = '1.0';
            $strokeColor = '';
            $strokeAlpha = '1.0';

            // Parse color and alpha helper
            $parseColorAndAlpha = function($colorAttr, &$color, &$alpha) {
                if (!$colorAttr || $colorAttr == 'none') {
                    $color = '@android:color/transparent';
                    $alpha = '0.0';
                    return;
                }
                if (strpos($colorAttr, 'rgba') !== false) {
                    // rgba(255,255,255,0.15)
                    preg_match('/rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*([\d\.]+)\s*\)/', $colorAttr, $rgba);
                    if ($rgba) {
                        $color = sprintf("#%02X%02X%02X", $rgba[1], $rgba[2], $rgba[3]);
                        $alpha = $rgba[4];
                    }
                } elseif (strpos($colorAttr, 'rgb') !== false) {
                    // rgb(255,255,255)
                    preg_match('/rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)/', $colorAttr, $rgb);
                    if ($rgb) {
                        $color = sprintf("#%02X%02X%02X", $rgb[1], $rgb[2], $rgb[3]);
                        $alpha = '1.0';
                    }
                } else {
                    $color = $colorAttr;
                    $alpha = '1.0';
                }
            };

            $parseColorAndAlpha($fillColorAttr, $fillColor, $fillAlpha);
            if ($opacityAttr !== '') {
                $fillAlpha = (float)$fillAlpha * (float)$opacityAttr;
            }

            if ($strokeColorAttr) {
                $parseColorAndAlpha($strokeColorAttr, $strokeColor, $strokeAlpha);
                if ($opacityAttr !== '') {
                    $strokeAlpha = (float)$strokeAlpha * (float)$opacityAttr;
                }
            }

            // Generate pathData based on node type
            switch ($nodeName) {
                case 'path':
                    $pathData = $el->getAttribute('d');
                    break;

                case 'circle':
                    $cx = (float)$el->getAttribute('cx');
                    $cy = (float)$el->getAttribute('cy');
                    $r = (float)$el->getAttribute('r');
                    $pathData = "M " . ($cx - $r) . ",{$cy} A {$r},{$r} 0 1,1 " . ($cx + $r) . ",{$cy} A {$r},{$r} 0 1,1 " . ($cx - $r) . ",{$cy} Z";
                    break;

                case 'ellipse':
                    $cx = (float)$el->getAttribute('cx');
                    $cy = (float)$el->getAttribute('cy');
                    $rx = (float)$el->getAttribute('rx');
                    $ry = (float)$el->getAttribute('ry');
                    $pathData = "M " . ($cx - $rx) . ",{$cy} A {$rx},{$ry} 0 1,1 " . ($cx + $rx) . ",{$cy} A {$rx},{$ry} 0 1,1 " . ($cx - $rx) . ",{$cy} Z";
                    break;

                case 'rect':
                    $x = (float)$el->getAttribute('x');
                    $y = (float)$el->getAttribute('y');
                    $w = (float)$el->getAttribute('width');
                    $h = (float)$el->getAttribute('height');
                    $rxAttr = $el->getAttribute('rx');
                    if ($rxAttr !== '') {
                        $rx = (float)$rxAttr;
                        $pathData = "M " . ($x + $rx) . ",{$y} h " . ($w - $rx * 2) . " a {$rx},{$rx} 0 0,1 {$rx},{$rx} v " . ($h - $rx * 2) . " a {$rx},{$rx} 0 0,1 -{$rx},{$rx} h -" . ($w - $rx * 2) . " a {$rx},{$rx} 0 0,1 -{$rx},-{$rx} v -" . ($h - $rx * 2) . " a {$rx},{$rx} 0 0,1 {$rx},-{$rx} Z";
                    } else {
                        $pathData = "M {$x},{$y} h {$w} v {$h} h -{$w} Z";
                    }
                    break;

                case 'line':
                    $x1 = (float)$el->getAttribute('x1');
                    $y1 = (float)$el->getAttribute('y1');
                    $x2 = (float)$el->getAttribute('x2');
                    $y2 = (float)$el->getAttribute('y2');
                    $pathData = "M {$x1},{$y1} L {$x2},{$y2}";
                    break;
            }

            // Write path element
            $xml .= "    <path\n";
            $xml .= "        android:pathData=\"{$pathData}\"\n";
            if ($fillColor != '@android:color/transparent') {
                $xml .= "        android:fillColor=\"{$fillColor}\"\n";
                if ($fillAlpha != '1.0' && $fillAlpha != '1') {
                    $xml .= "        android:fillAlpha=\"{$fillAlpha}\"\n";
                }
            }
            if ($strokeColor && $strokeColor != '@android:color/transparent') {
                $xml .= "        android:strokeColor=\"{$strokeColor}\"\n";
                if ($strokeAlpha != '1.0' && $strokeAlpha != '1') {
                    $xml .= "        android:strokeAlpha=\"{$strokeAlpha}\"\n";
                }
                $xml .= "        android:strokeWidth=\"" . ($strokeWidth ? $strokeWidth : 1) . "\"\n";
            }
            $xml .= "        android:strokeLineCap=\"round\"\n";
            $xml .= "        android:strokeLineJoin=\"round\" />\n";
        }
    }

    $xml .= "</vector>\n";

    $outputFile = "$outputDir/ic_onboarding_{$slideNumber}.xml";
    file_put_contents($outputFile, $xml);
    echo "Saved slide $slideNumber VectorDrawable to: $outputFile\n";
    $slideNumber++;
}

echo "Successfully converted all SVGs to VectorDrawables.\n";
