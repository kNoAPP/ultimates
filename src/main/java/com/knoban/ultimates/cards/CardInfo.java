package com.knoban.ultimates.cards;

import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CardInfo {

    Material material() default Material.BARRIER;
    String name() default "N/A";
    String display() default "§cN/A";
    String[] description() default "N/A";
    PrimalSource source() default PrimalSource.NONE;
    Tier tier() default Tier.COMMON;

}
