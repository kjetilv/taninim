package taninim.kudu.server;

import com.github.kjetilv.uplift.hash.Hash;

import static com.github.kjetilv.uplift.hash.HashKind.K128;

record LibraryRequest(Hash<K128> token) {
}
