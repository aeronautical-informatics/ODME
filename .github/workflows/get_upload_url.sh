#!/bin/bash

upload_url=$(curl -sS -H "Authorization: Bearer ${{ secrets.GITHUB_TOKEN }}" -X GET "https://api.github.com/repos/${{ github.repository }}/releases/${{ github.run_number }}" | jq -r '.upload_url' | sed 's/{?name,label}//')
echo "::set-output name=upload_url::$upload_url"
