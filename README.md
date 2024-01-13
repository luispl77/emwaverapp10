# Repository is now finished for version 1.0 and OUTDATED. New Repo: luispl77/ismwaverapp


# EMWaver Android App version 1.0
This android app serves as communication interface to the CC1101 radio chip from Texas Instruments. 
It allows to take advantage of most of the features of the packet engine that the radio chip provides.

## Features
- Terminal Fragment
- Packet Mode Fragment
- Flash Fragment

### Terminal Fragment
All CDC (Communication Device Class) USB Serial communications will be shown in this fragment, both IN or OUT.
It's in this fragment that the user connects the EMWaver device to the port.

### Packet Mode Fragment
In this fragment the user can choose between Tx (Transmit) and Rx (Receive) modes of the radio.
Packet structure:
<img width="512" alt="image" src="https://github.com/luispl77/emwaverapp10/assets/81360502/6e1c6eef-08fa-4add-8e6f-4c9a68c90611">

There are several configurations the user can choose for each mode:
#### Common configurations
- Receiver Channel Filter Bandwidth
- Frequency Offset Compensation
- A programmable number of preamble bytes
- A two byte synchronization (sync) word. Can be duplicated to give a 4-byte sync word (recommended). It is not possible to only insert preamble or only insert a sync word
- A CRC checksum computed over the data field.
- Data Whitening

#### Packet Handling in Transmit Mode
The payload that is to be transmitted must be written into the TX FIFO.
The first byte written must be the length byte when variable packet length is enabled.
The length byte has a value equal to the payload of the packet (including the optional address byte).
If address recognition is enabled on the receiver, the second byte written to the TX FIFO must be the address byte.
If fixed packet length is enabled, the first byte written to the TX FIFO should be the address (assuming the receiver uses address recognition).

#### Packet Handling in Receive Mode
In receive mode, the demodulator and packet handler will search for a valid preamble and the sync word.
When found, the demodulator has obtained both bit and byte synchronization and will receive the first payload byte.
