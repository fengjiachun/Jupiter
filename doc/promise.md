
        // service2依赖service1的返回结果
        // service3依赖service2的返回结果

        DeferredVoidPromise promise = new DeferredVoidPromise();

        promise.then(new InvokeDonePipe<Void, String>() {

                    @Override
                    public void doInPipe(Void result) {
                        service1.hello1();
                    }
        }).then(new InvokeDonePipe<String, String>() {

            @Override
            public void doInPipe(String service1Result) {
                service2.hello2(service1Result);
            }
        }).then(new InvokeDonePipe<String, String>() {

            @Override
            public void doInPipe(String service2Result) {
                service3.hello3(service2Result);
            }
        })
        .then(new InvokeDone<String>() {

            @Override
            public void onDone(String service3Result) {
                System.out.println("step3 result = " + service3Result);
            }
        }, new InvokeFail() {

            @Override
            public void onFail(Throwable cause) {
                System.err.println("step3 fail:" + cause);
            }
        });

        promise.resolve(); // start

[详细例子参照这里](https://github.com/fengjiachun/Jupiter/blob/master/jupiter-example/src/main/java/org/jupiter/example/round/HelloJupiterPromiseClient.java)