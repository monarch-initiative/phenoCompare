#!/usr/local/bin/perl

# alpha2numeric.pl
# author: Hannah Blau

# converts file of gene names to file of ENTREZ gene ids, preserving structure of input file
# -- input filename is parameter to program
# -- output filename is same as input filename but prefaced by ENTREZ_
# -- looks up gene names in file human_protein_coding_genes.tsv, writes corresponding numeric
# id to output file
# -- any gene name that cannot be found in human_protein_coding_genes.tsv is printed unchanged
# in the output file

use Modern::Perl '2015';
use autodie;

die "Command line argument is gene set filename\n" unless @ARGV == 1;
my $gene_names_file = shift;
my $gene_ids_file = 'ENTREZ_' . $gene_names_file;

open my $in_fh, '<', $gene_names_file;

say "Overwriting contents of output file $gene_ids_file" if -e $gene_ids_file;
open my $out_fh, '>', $gene_ids_file;

while (<$in_fh>) {
    # print contents of input file to console as you progress through the file
    printf "%s", $_;
    chomp;
    my @fields = split /\t/;
    my @entrez_ids;
    for my $field (@fields) {
        my $entrez_id = `fgrep -w $field human_protein_coding_genes.tsv | cut -f1`;
        if ($entrez_id) {
            chomp $entrez_id;
        }
        else {
            $entrez_id = $field;
        }
        push @entrez_ids, $entrez_id;
    }
    my $last_index = @entrez_ids - 1;
    for my $entrez_id (@entrez_ids[0..$last_index - 1]) {
        printf $out_fh "%s\t", $entrez_id;
    }
    say $out_fh $entrez_ids[$last_index];
}

close $in_fh;
close $out_fh;
