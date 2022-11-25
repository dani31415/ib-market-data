package dev.damaso.market.commands.snapshot;

import java.util.HashMap;
import java.util.Map;

public class SnapshotState {
    public int cNormal = 0;
    public int cClosed = 0;
    public int cHalted = 0;
    Map<String,Integer> conidToSymbol = new HashMap<>();
}
