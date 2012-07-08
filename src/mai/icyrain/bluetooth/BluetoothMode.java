package mai.icyrain.bluetooth;

/**
 * Bluetooth operation modes.
 * 
 * @author thanhhaipmai@gmail.com (Thanh Hai Mai)
 */
public enum BluetoothMode {
  /**
   * A master can initiates a connection to another device, and listen to no connection request.
   * It can send and receive messages after the connection is established.
   */
  Master,
  /**
   * A peer can initiates a connection and listens to connection request.
   * It can send and receive messages after the connection is established.
   * 
   * @note: not implemented.
   */
  Peer,
  /**
   * A server can only listens and waits for a clients to connect too. It can handle multiple
   * connections.
   * It can send and receive messages after the connection is established.
   * 
   * @note: not implemented.
   */
  Server,
  /**
   * A slave can only listens and waits for a master to connect too. It cannot initiates a
   * connection.
   * It can send and receive messages after the connection is established.
   */
  Slave,
}
