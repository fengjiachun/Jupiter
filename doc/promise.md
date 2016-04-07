
        // service2依赖service1的返回结果
        // service3依赖service2的返回结果

        DeferredVoidPromise promise = new DeferredVoidPromise();

        promise.then(new InvokeDonePipe<Void, String>() {

                    @Override
                    public void doInPipe(Void result) {
                        System.err.println("step1 doing...");

                        service1.hello1();
                    }
        }).then(new InvokeDonePipe<String, String>() {

            @Override
            public void doInPipe(String result1) {
                System.err.println("step1 result = " + result1);

                System.err.println("step2 doing...");
                service2.hello2();
            }
        }).then(new InvokeDonePipe<String, String>() {

            @Override
            public void doInPipe(String result2) {
                System.err.println("step2 result =  " + result2);

                System.err.println("step3 doing");
                service3.hello3();
            }
        })
        .then(new InvokeDone<String>() {

            @Override
            public void onDone(String result3) {
                System.err.println("step3 result =  " + result3);
            }
        }, new InvokeFail() {

            @Override
            public void onFail(Throwable cause) {
                System.err.println("step3 fail:" + cause);
            }
        });

        promise.resolve(); // start

[详细例子参照这里](https://github.com/fengjiachun/Jupiter/blob/master/jupiter-example/src/main/java/org/jupiter/example/round/HelloJupiterPromiseClient.java)