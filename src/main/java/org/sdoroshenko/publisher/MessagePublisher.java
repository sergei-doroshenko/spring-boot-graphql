package org.sdoroshenko.publisher;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.observables.ConnectableObservable;
import org.sdoroshenko.model.Message;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessagePublisher {

    private final Flowable<Message> publisher;

    public MessagePublisher() {
        Observable<Message> messageObservable = Observable.create(emitter -> {

            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleAtFixedRate(newMessage(emitter), 0, 2, TimeUnit.SECONDS);

        });

        ConnectableObservable<Message> connectableObservable = messageObservable.share().publish();
        connectableObservable.connect();

        publisher = connectableObservable.toFlowable(BackpressureStrategy.BUFFER);
    }

    private Runnable newMessage(ObservableEmitter<Message> emitter) {
        return () -> {
                emitMessages(emitter, Arrays.asList(new Message(2L, "test")));
        };
    }

    private void emitMessages(ObservableEmitter<Message> emitter, List<Message> messagesUpdates) {
        for (Message update : messagesUpdates) {
            try {
                emitter.onNext(update);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    public Flowable<Message> getPublisher() {
        return publisher;
    }

    public Flowable<Message> getPublisher(List<String> fields) {
        return publisher.filter(update -> fields.contains(update.getId()) || true);
    }
}
