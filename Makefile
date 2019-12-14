COMPILE=javac -encoding ISO-8859-1 Join/Main.java
RUN_MAIN=java -Xmx50m -Xmx50m Join/Main
CREATE_RAND_FILES=java -jar FileGenerator.jar  file1.txt 100000 file2.txt 500000


.PHONY : delete_txt fresh clean all

create_rand_files:
	$(CREATE_RAND_FILES)

test_a:
	$(COMPILE)
	$(RUN_MAIN) A file1.txt sortedFile1.txt TmpFolder
	sort file1.txt -k1 -k2 -k3 > sortedFile1ByTerminal.txt
	diff sortedFile1ByTerminal.txt sortedFile1.txt

test_b:
	$(COMPILE)
	$(RUN_MAIN) "B" file1.txt file2.txt joined.txt tmpFolder
	sort file1.txt -k1 -k2 -k3 > sortedFile1ByTerminal.txt
	sort file2.txt -k1 -k2 -k3 > sortedFile2ByTerminal.txt
	join -1 1 -2 1 sortedFile1ByTerminal.txt sortedFile2ByTerminal.txt > joinedByTerm.txt
	diff joinedByTerm.txt joined.txt

test_c:
	sort file1.txt -k1 -k2 -k3 > sortedFile1ByTerminal.txt
	sort file2.txt -k1 -k2 -k3 > sortedFile2ByTerminal.txt
	join -1 1 -2 1 sortedFile1ByTerminal.txt sortedFile2ByTerminal.txt > joinedByTerm.txt
	cat joinedByTerm.txt | grep "2" > selectedByTerm.txt
	$(RUN_MAIN) "C" file1.txt file2.txt selectedByUs.txt "2" TmpFolder
	diff selectedByUs.txt selectedByTerm.txt

test_d:
	sort file1.txt -k1 -k2 -k3 > sortedFile1ByTerminal.txt
	sort file2.txt -k1 -k2 -k3 > sortedFile2ByTerminal.txt
	join -1 1 -2 1 sortedFile1ByTerminal.txt sortedFile2ByTerminal.txt > joinedByTerm.txt
	cat joinedByTerm.txt | grep "2" > selectedByTerm.txt
	$(RUN_MAIN) "D" file1.txt file2.txt selectedByUs.txt "2" tmpFolder
	diff selectedByUs.txt selectedByTerm.txt

delete_txt:
	rm -r *.txt tmpFolder/*.txt



