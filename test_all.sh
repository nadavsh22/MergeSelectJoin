set -e


make create_rand_files
make test_a
make delete_txt

make create_rand_files
make test_b
make delete_txt

make create_rand_files
make test_c
make delete_txt

make create_rand_files
make test_d
make delete_txt

