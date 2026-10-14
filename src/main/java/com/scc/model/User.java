package com.scc.model;

public record User (int user_id, String fullname, String username, String password, String email, String contact_number, String address, String role,  String status, String datecreated ) {
    @Override
    public String toString(){
        return String.format("[%d] %s", user_id, username, role, status);

    }


}
