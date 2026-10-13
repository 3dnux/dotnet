#include "xex_info.h"
#include <fcntl.h>
#include <cstdio>
#include <fstream>
#include <iterator>
int main(int argc,char**argv){
  std::ifstream t("expected_title.txt",std::ios::binary), i("expected_icon.bin",std::ios::binary);
  std::string et((std::istreambuf_iterator<char>(t)),{}), ei((std::istreambuf_iterator<char>(i)),{});
  int bad=0;
  for(int a=1;a<argc;a++){
    int fd=open(argv[a],O_RDONLY); XexMeta m; bool ok=ReadXexMeta(fd,&m);
    bool good=ok&&m.title_id==0x4D5307E6&&m.title==et&&std::string(m.icon.begin(),m.icon.end())==ei;
    printf("%-14s ok=%d id=%08X title='%s' icon=%zu => %s\n",argv[a],ok,m.title_id,m.title.c_str(),m.icon.size(),good?"PASS":"FAIL");
    bad+=!good;
  }
  return bad;
}
