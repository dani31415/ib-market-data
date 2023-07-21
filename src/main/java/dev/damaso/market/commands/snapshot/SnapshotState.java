package dev.damaso.market.commands.snapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import dev.damaso.market.entities.SymbolSnapshot;

public class SnapshotState {
    public int cNormal = 0;
    public int cClosed = 0;
    public int cHalted = 0;
    public int cError = 0;
    Map<String,Integer> conidToSymbol = new HashMap<>();
    public List<SymbolSnapshot> openMarketData = new Vector<>();
}
