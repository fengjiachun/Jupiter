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
package org.jupiter.rpc.consumer.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jupiter.common.util.Requires;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @see InvokeFutureGroup
 *
 * @author jiachun.fjc
 */
public class DefaultInvokeFutureGroup<V> implements InvokeFutureGroup<V> {

    private final DefaultInvokeFuture<V>[] futures;
    private volatile CompletableFuture<V>[] cfs;

    public static <T> DefaultInvokeFutureGroup<T> with(DefaultInvokeFuture<T>[] futures) {
        return new DefaultInvokeFutureGroup<>(futures);
    }

    private DefaultInvokeFutureGroup(DefaultInvokeFuture<V>[] futures) {
        Requires.requireTrue(futures != null && futures.length > 0, "empty futures");
        this.futures = futures;
    }

    @Override
    public Class<V> returnType() {
        return futures[0].returnType();
    }

    @Override
    public V getResult() throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    public InvokeFuture<V>[] futures() {
        return futures;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<V>[] toCompletableFutures() {
        if (this.cfs == null) {
            CompletableFuture<V>[] cfs = new CompletableFuture[futures.length];
            System.arraycopy(futures, 0, cfs, 0, futures.length);
            this.cfs = cfs;
        }
        return this.cfs;
    }

    @Override
    public <U> CompletionStage<U> thenApply(Function<? super V, ? extends U> fn) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(Function<? super V, ? extends U> fn) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(Function<? super V, ? extends U> fn, Executor executor) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> thenAccept(Consumer<? super V> action) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super V> action) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super V> action, Executor executor) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> thenRun(Runnable action) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> thenRunAsync(Runnable action) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
        throw reject();
    }

    @Override
    public <U, V1> CompletionStage<V1> thenCombine(CompletionStage<? extends U> other, BiFunction<? super V, ? super U, ? extends V1> fn) {
        throw reject();
    }

    @Override
    public <U, V1> CompletionStage<V1> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super V, ? super U, ? extends V1> fn) {
        throw reject();
    }

    @Override
    public <U, V1> CompletionStage<V1> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super V, ? super U, ? extends V1> fn, Executor executor) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super V, ? super U> action) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super V, ? super U> action) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super V, ? super U> action, Executor executor) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> applyToEither(CompletionStage<? extends V> other, Function<? super V, U> fn) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends V> other, Function<? super V, U> fn) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends V> other, Function<? super V, U> fn, Executor executor) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> acceptEither(CompletionStage<? extends V> other, Consumer<? super V> action) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends V> other, Consumer<? super V> action) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends V> other, Consumer<? super V> action, Executor executor) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        throw reject();
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> thenCompose(Function<? super V, ? extends CompletionStage<U>> fn) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(Function<? super V, ? extends CompletionStage<U>> fn) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(Function<? super V, ? extends CompletionStage<U>> fn, Executor executor) {
        return null;
    }

    @Override
    public CompletionStage<V> exceptionally(Function<Throwable, ? extends V> fn) {
        throw reject();
    }

    @Override
    public CompletionStage<V> whenComplete(BiConsumer<? super V, ? super Throwable> action) {
        throw reject();
    }

    @Override
    public CompletionStage<V> whenCompleteAsync(BiConsumer<? super V, ? super Throwable> action) {
        throw reject();
    }

    @Override
    public CompletionStage<V> whenCompleteAsync(BiConsumer<? super V, ? super Throwable> action, Executor executor) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> handle(BiFunction<? super V, Throwable, ? extends U> fn) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> handleAsync(BiFunction<? super V, Throwable, ? extends U> fn) {
        throw reject();
    }

    @Override
    public <U> CompletionStage<U> handleAsync(BiFunction<? super V, Throwable, ? extends U> fn, Executor executor) {
        throw reject();
    }

    @Override
    public CompletableFuture<V> toCompletableFuture() {
        throw reject();
    }

    private static UnsupportedOperationException reject() {
        return new UnsupportedOperationException("not a supported operation");
    }
}
