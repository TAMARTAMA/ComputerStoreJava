package Entities;

import java.time.LocalDate;
import java.util.Objects;

public class Customer {
    private String name;
    private String address;
    private long id;
    private Identity identity;
    private LocalDate localDate;

    public Customer(String name, String address, int id, Identity identity,LocalDate localDate) {
        this.name = name;
        this.address = address;
        this.id = id;
        this.identity = identity;
        this.localDate = localDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public Customer(String name, String address, int id, String username, String password, int year, int month, int day) {
        this.name = name;
        this.address = address;
        this.id = id;
        this.identity=new Identity(username,password);
//        this.identity.userName=username;
//        this.identity.password=password;
        localDate=LocalDate.of(year,month,day);
    }

    public class Identity{

        private String userName;
        private String password;
        public Identity(String userName, String password){
            this.userName = userName;
        }
        public String getUserName() {
            return userName;
        }
        public void setUserName(String userName) {
            this.userName = userName;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String toString() {
            return "Identity{" +
                    "userName='" + userName + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Identity identity = (Identity) o;
            return Objects.equals(userName, identity.userName) && Objects.equals(password, identity.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userName, password);
        }
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Entities.Customer{" +
                "name='" + name + '\'' +
                "identity=" + identity +
                ", address='" + address + '\'' +
                ", id=" + id +
                ", identity=" + identity +
                ", localDate=" + localDate +
                '}';
    }
}
