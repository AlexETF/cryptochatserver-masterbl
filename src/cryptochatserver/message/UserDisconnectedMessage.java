/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cryptochatserver.message;

import java.io.ByteArrayOutputStream;

/**
 *
 * @author ZM
 */
public class UserDisconnectedMessage extends Message {
    
    private String _username;
    
    public UserDisconnectedMessage(String username){
        super(Message.USER_DISCONNECTED);
        _username = username;
    }

    @Override
    public byte[] getMessageData() throws Exception {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        target.write(new byte[]{_type});
        target.write(Message.MESSAGE_PART_DELIMITER);
        target.write(_username.getBytes("ISO-8859-1"));;
        return target.toByteArray();
    }
    
}
