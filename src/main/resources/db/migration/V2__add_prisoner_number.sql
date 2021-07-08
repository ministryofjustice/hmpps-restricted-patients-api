
alter table restricted_patients add prisoner_number varchar(7) not null;

create index restricted_patients_prisoner_number_idx on restricted_patients(prisoner_number)