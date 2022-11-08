Performance Test
```shell script
ghz --insecure -c 100 -z 30s --connections 100 \
  --proto kert-grpc/src/test/proto/echo.proto \
  --call ws.leap.kert.test.Echo.unary \
  -d '{"id":1, "value":"hello"}' \
  0.0.0.0:8550
```
