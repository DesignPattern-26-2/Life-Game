package com.holub.life.patterns;

/** Simple factory (one static creation method with a switch) for the preset Life patterns; not the GoF Factory Method. */
public final class PatternFactory {
    public enum Type { GLIDER, BLINKER, BLOCK, TOAD }

    private PatternFactory() { }

    public static LifePattern create(Type type) {
        if (type == null)
            throw new IllegalArgumentException("Pattern type must not be null");

        switch (type) {
        case GLIDER:  return new GliderPattern();
        case BLINKER: return new BlinkerPattern();
        case BLOCK:   return new BlockPattern();
        case TOAD:    return new ToadPattern();
        default:      throw new IllegalArgumentException("Unsupported pattern: " + type);
        }
    }
}
