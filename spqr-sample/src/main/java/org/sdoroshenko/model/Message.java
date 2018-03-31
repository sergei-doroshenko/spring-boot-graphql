package org.sdoroshenko.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Message {
    @Getter @Setter private Long id;
    @Getter @Setter private String body;
}
