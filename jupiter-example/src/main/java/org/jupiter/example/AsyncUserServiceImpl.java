/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jupiter.example;

import java.util.concurrent.CompletableFuture;

import org.jupiter.rpc.ServiceProviderImpl;

/**
 * jupiter
 * org.jupiter.example
 *
 * @author jiachun.fjc
 */
@ServiceProviderImpl(version = "1.0.0.daily")
public class AsyncUserServiceImpl implements AsyncUserService {

    @Override
    public User syncCreateUser() {
        return User.createUser();
    }

    @Override
    public CompletableFuture<User> createUser() {
        CompletableFuture<User> f = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            f.complete(User.createUser());
        }).start();
        return f;
    }

    @Override
    public MyCompletableFuture<User> createUser2() {
        MyCompletableFuture<User> f = new MyCompletableFuture<>();
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            f.complete(User.createUser());
        }).start();
        return f;
    }
}
