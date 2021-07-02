drop table if exists restricted_patients;

create table restricted_patients
(
    id                     serial primary key,
    from_location_id       varchar(5)  not null,
    hospital_location_code varchar(10) not null,
    supporting_prison_id   varchar(5)  not null,
    discharge_time         timestamp   not null,
    comment_text           varchar(240),
    active                 boolean     not null,
    create_user_id         varchar(32),
    create_datetime        timestamp   not null,
    modify_datetime        timestamp,
    modify_user_id         varchar(32)
);

comment
on table restricted_patients is 'Store information on prisoners currently in hospital';

create index restricted_patients_discharged_hospital_idx on restricted_patients(hospital_location_code)