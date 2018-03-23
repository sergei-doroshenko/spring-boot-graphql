package org.sdoroshenko.publisher;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.observables.ConnectableObservable;
import lombok.Getter;
import org.reactivestreams.Publisher;
import org.sdoroshenko.model.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class MessagePublisher {

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    @Getter
    private final Publisher<Message> publisher;

    public MessagePublisher() {
        Observable<Message> observable = Observable.create((ObservableEmitter<Message> emitter) -> {
            executorService.scheduleAtFixedRate(emitMessages(emitter), 0, 2, TimeUnit.SECONDS);
        });

        ConnectableObservable<Message> connectableObservable = observable.share().publish();
        connectableObservable.connect();

        publisher = connectableObservable.toFlowable(BackpressureStrategy.BUFFER);
    }

    private Runnable emitMessages(ObservableEmitter<Message> emitter) {
        return () -> emitter.onNext(new Message(103L, "test: " + System.currentTimeMillis()));
    }

    public Publisher<Message> getPublisher(List<String> messageFilter) {
        return publisher;
    }
}
