package org.sdoroshenko.publisher;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.observables.ConnectableObservable;
import lombok.Getter;
import org.sdoroshenko.model.Message;
import org.sdoroshenko.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class MessageStreamer {
    @Autowired
    private MessageRepository messageRepository;

    @Getter
    private final Flowable<Message> publisher;
    @Getter
    private ObservableEmitter<Message> emitter;

    public MessageStreamer() {
        Observable<Message> observable = Observable.create((ObservableEmitter<Message> emitter) -> {
            this.emitter = emitter;
        });

        ConnectableObservable<Message> connectableObservable = observable.share().publish();
        connectableObservable.connect();

        publisher = connectableObservable.toFlowable(BackpressureStrategy.BUFFER);
    }

    public Message emitMessage(Message message) {
        Message saved = messageRepository.save(message);
        emitter.onNext(saved);
        return saved;
    }
}
