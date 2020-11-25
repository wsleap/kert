```shell script
ghz --insecure -c 100 -z 30s --connections 100 \                                            7128  21:31:13 
  --proto kettle-grpc/src/test/proto/echo.proto \
  --call ws.leap.kettle.test.Echo.unary \
  -d '{"id":1, "value":"hello"}' \
  0.0.0.0:8888
```
