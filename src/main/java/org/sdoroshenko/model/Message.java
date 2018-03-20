package org.sdoroshenko.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @Getter @Setter private Long id;
    @Getter @Setter private String body;
}
