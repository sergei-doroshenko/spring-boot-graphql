package org.sdoroshenko.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Message {
    @Getter @Setter private Long id;
    @Getter @Setter private String body;
}
