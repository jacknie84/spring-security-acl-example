insert into acl_class (id, class, class_id_type) values (1, 'Root', 'java.lang.String');
insert into acl_sid (id, type, sid) values (1, 'GRANTED_AUTHORITY', 'ROLE_ADMIN');
insert into acl_object_identity (id, object_id_identity, entries_inheriting, parent_object, object_id_class, owner_sid) values (1, 'root', true, null, 1, 1);
