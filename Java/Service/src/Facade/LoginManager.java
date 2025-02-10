package Facade;

public class LoginManager
{
    public ClientFacade login(String email ,String password, ClientTpye clientType) throws Exception{
        boolean boolLogin = false;
        ClientFacade clientService = null;
        switch (clientType) {
            case Administrator:
                clientService = new AdminFacade();
                break;
            case Customer:
                clientService = new CustomerFacade();
                break;
        }
        boolLogin = clientService.Login(email, password);
        if(!boolLogin)
            return null;
        else
            return clientService;
    }

}
