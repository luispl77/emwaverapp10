//script 2
function initReceive() {
    CC1101.sendInitRx();
    CC1101.setNumPreambleBytes(5);
    CC1101.setModulation(CC1101.MOD_FSK);
    var mercedes_sync_word = [0xAA, 0xA9];
    CC1101.setSyncWord(mercedes_sync_word);
    CC1101.setDataRate(2000);
    CC1101.setDeviation(15000);
}

function getMercedesSignal() {
    var receivedBytes = CC1101.receiveData();
    Console.print("signal: " + CC1101.bytesToHexString(receivedBytes) + "\n");
}


initReceive();
getMercedesSignal();

