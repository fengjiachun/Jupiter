
        // service2依赖service1的返回结果
        // service3依赖service2的返回结果

        service1.method1(); // step1. 先调用service1

        InvokePromiseContext.currentPromise()
                .then(new InvokeDonePipe() {

                    @Override
                    public void doInPipe(Object result1) {
                        // result1为service1的调用返回值
                        Object parameter2 = result1;
                        service2.method2(parameter2); // step2. 再调用service2
                    }
                })
                .then(new InvokeDonePipe() {

                    @Override
                    public void doInPipe(Object result2) {
                        // result2为service2的调用返回值
                        Object parameter3 = result2;
                        service3.method3(parameter3); // step3. 再调用service3
                    }
                })
                .then(new InvokeDone() {

                    @Override
                    public void onDone(Object result3) {
                        // result3为service3的调用返回值
                    }
                }, new InvokeFail() {

                    @Override
                    public void onFail(Throwable cause) {
                        // 处理失败
                    }
                });

[详细例子参照这里](https://github.com/fengjiachun/Jupiter/blob/master/jupiter-example/src/main/java/org/jupiter/example/round/HelloJupiterPromiseClient.java)