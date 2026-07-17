<?php header("Content-Type: text/plain"); echo "=== ROOT ===\n"; print_r(glob("*")); echo "\n=== AUTH ===\n"; print_r(glob("auth/*"));
