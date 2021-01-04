package com.knoban.ultimates.cardholder;

import com.knoban.ultimates.cards.Card;

import java.util.Collection;

public interface CardCallback {

    void call(boolean success, Collection<Card> toEquip);
}
