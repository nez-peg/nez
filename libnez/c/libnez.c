#include "libnez.h"
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <time.h>

#define NEZ_DEBUG 0

void nez_PrintErrorInfo(const char *errmsg) {
  fprintf(stderr, "%s\n", errmsg);
  exit(EXIT_FAILURE);
}

void nez_consume(ParsingContext ctx) {
  if(ctx->pos < ctx->input_size) {
    // fprintf(stderr, "%ld\n", ctx->pos);
    if(ctx->inputs[ctx->pos] != 0) {
      ctx->pos++;
    }
  }
  else {
    nez_PrintErrorInfo("input over flow");
  }
}

int nez_unconsume_check(ParsingContext ctx, long pos) {
  if(ctx->pos < ctx->input_size) {
    if(ctx->pos == pos) {
      return 1;
    }
    return 0;
  }
  else {
    nez_PrintErrorInfo("input over flow");
  }
  return 0;
}

void nez_backtrack(ParsingContext ctx, long pos) {
  if(pos != ctx->pos) {
    if(pos < ctx->input_size) {
      if(pos > ctx->pos) {
        nez_PrintErrorInfo("backtrack error");
      }
      ctx->pos = pos;
    }
    else {
      fprintf(stderr, "%ld, %ld, %ld\n", ctx->pos, ctx->input_size, pos);
      nez_PrintErrorInfo("backtrack error");
    }
  }
}

int nez_not_match(ParsingContext ctx, char c) {
  return ctx->inputs[ctx->pos] != c;
}

void dump_pego(ParsingObject *pego, char *source, int level) {
  int i;
  long j;
  if (pego[0]) {
    for (i = 0; i < level; i++) {
      fprintf(stderr, "  ");
    }
    fprintf(stderr, "{%s ", pego[0]->tag);
    if (pego[0]->child_size == 0) {
      fprintf(stderr, "'");
      if (pego[0]->value == NULL) {
        for (j = pego[0]->start_pos; j < pego[0]->end_pos; j++) {
          fprintf(stderr, "%c", source[j]);
        }
      } else {
        fprintf(stderr, "%s", pego[0]->value);
      }
      fprintf(stderr, "'");
    } else {
      fprintf(stderr, "\n");
      for (j = 0; j < pego[0]->child_size; j++) {
        dump_pego(&pego[0]->child[j], source, level + 1);
      }
      for (i = 0; i < level; i++) {
        fprintf(stderr, "  ");
      }
    }
    fprintf(stderr, "}\n");
  } else {
    fprintf(stderr, "%p tag:null\n", pego);
  }
}

char *loadFile(const char *filename, size_t *length) {
  size_t len = 0;
  FILE *fp = fopen(filename, "rb");
  char *source;
  if (!fp) {
    nez_PrintErrorInfo("fopen error: cannot open file");
    return NULL;
  }
  fseek(fp, 0, SEEK_END);
  len = (size_t)ftell(fp);
  fseek(fp, 0, SEEK_SET);
  source = (char *)malloc(len + 1);
  if (len != fread(source, 1, len, fp)) {
    nez_PrintErrorInfo("fread error: cannot read file collectly");
  }
  source[len] = '\0';
  fclose(fp);
  *length = len;
  return source;
}

ParsingContext nez_CreateParsingContext(const char *filename) {
  ParsingContext ctx = (ParsingContext)malloc(sizeof(struct ParsingContext));
  ctx->pos = ctx->input_size = 0;
  ctx->inputs = loadFile(filename, &ctx->input_size);
  ctx->cur = ctx->inputs;
  ctx->choiceCount = 0;
  return ctx;
}

void nez_DisposeParsingContext(ParsingContext ctx) {
  free(ctx->inputs);
  free(ctx);
}

void nez_DisposeObject(ParsingObject pego) {
  ParsingObject *child;
  assert(pego != NULL);
  child = pego->child;
  pego->child = NULL;
  if (child) {
    int child_size = pego->child_size;
    for (int i = 0; i < child_size; i++) {
      nez_DisposeObject(child[i]);
    }
    free(child);
  }
}

ParsingLog nez_newLog(ParsingContext ctx) {
  ParsingLog l;
  if(ctx->unusedLog == NULL) {
    l = (ParsingLog)malloc(sizeof(struct ParsingLog));
  }
  else {
    l = ctx->unusedLog;
    ctx->unusedLog = ctx->unusedLog->next;
  }
  l->next = NULL;
  return l;
}

ParsingObject nez_newObject(ParsingContext ctx, const char *start) {
  ParsingObject o;
  if(ctx->unusedObject == NULL) {
    o = (ParsingObject)malloc(sizeof(struct ParsingObject));
  }
  else {
    o = ctx->unusedObject;
    ctx->unusedObject = o->parent;
  }
  o->refc = 0;
  o->start_pos = start - ctx->inputs;
  o->end_pos = o->start_pos;
  o->tag = "#empty"; // default
  o->value = NULL;
  o->parent = NULL;
  o->child = NULL;
  o->child_size = 0;
  return o;
}

ParsingObject nez_newObject_(ParsingContext ctx, long start, long end,
                             const char* tag, const char* value) {
  ParsingObject o;
  o = (ParsingObject)malloc(sizeof(struct ParsingObject));
  o->refc = 0;
  o->start_pos = start;
  o->end_pos = end;
  o->tag = tag;
  o->value = value;
  o->parent = NULL;
  o->child = NULL;
  o->child_size = 0;
  return o;
}

void nez_unusedObject(ParsingContext ctx, ParsingObject o) {
  o->parent = ctx->unusedObject;
  ctx->unusedObject = o;
  if (o->child_size > 0) {
    for (int i = 0; i < o->child_size; i++) {
      nez_setObject(ctx, &(o->child[i]), NULL);
    }
    free(o->child);
    o->child = NULL;
  }
}

ParsingObject nez_setObject_(ParsingContext ctx, ParsingObject var, ParsingObject o) {
  if (var != NULL) {
    var->refc -= 1;
    if (var->refc == 0) {
      nez_unusedObject(ctx, var);
    }
  }
  if (o != NULL) {
    o->refc += 1;
  }
  return o;
}

void nez_setObject(ParsingContext ctx, ParsingObject *var, ParsingObject o) {
  nez_setObject_(ctx, *var, o);
  *var = o;
}

void nez_unusedLog(ParsingContext ctx, ParsingLog log) {
  if(log->type == LazyLink_T) {
    nez_setObject(ctx, &log->po, NULL);  
  }
  log->next = ctx->unusedLog;
  ctx->unusedLog = log;
}

int commitCount = 0;

ParsingObject commitNode(ParsingContext ctx, ParsingLog start, ParsingLog end,
                int objectSize, long spos, long epos,
                const char* tag, const char* value, ParsingObject po) {
  ParsingObject newnode = NULL;
  newnode = nez_setObject_(ctx, newnode, nez_newObject_(ctx, spos, epos, tag, value));
  if (objectSize > 0) {
    newnode->child = (ParsingObject *)calloc(sizeof(ParsingObject), objectSize);
    newnode->child_size = objectSize;
    if(po != NULL) {
      nez_setObject(ctx, &newnode->child[0], po);
    }
    ParsingLog next = NULL;
    int maxId = (objectSize - 1);
    for (ParsingLog cur = start; cur != end; cur = next) {
      next = cur->next;
#if NEZ_DEBUG
      fprintf(stderr, "Node[%d] type=%d,cur=%p next=%p\n", commitCount, cur->type, cur, cur->next);
#endif
      if(cur->type == LazyLink_T) {
        nez_setObject(ctx, &newnode->child[maxId - cur->pos], cur->po);
      }
      nez_unusedLog(ctx, cur);
    }
  }
  commitCount++;
  return newnode;
}

void nez_pushDataLog(ParsingContext ctx, int type, long pos,
                     int index, const char* value, ParsingObject po) {
  ParsingLog l = nez_newLog(ctx);
  l->type = type;
  l->next = ctx->logStack;
  ctx->logStack = l;
  ctx->logStackSize++;
  switch(type) {
    case LazyLink_T: {
      assert(po != NULL);
      l->pos = index;
      nez_setObject(ctx, &(l->po), po);
      break;
    }
    case LazyCapture_T: {
      l->pos = pos;
      break;
    }
    case LazyLeftJoin_T: {
      l->pos = pos;
      break;
    }
    case LazyNew_T: {
      l->pos = pos;
      break;
    }
    case LazyTag_T: {
      assert(value != NULL);
      l->value = value;
      break;
    }
    case LazyValue_T: {
      assert(value != NULL);
      l->value = value;
      break;
    }
  }
}

ParsingObject nez_commitLog(ParsingContext ctx, int mark) {
  ParsingLog start = ctx->logStack;
  if(!start) {
    return NULL;
  }
  ParsingLog cur = NULL;
  //assert(start->type == LazyCapture_T);
  int objectSize    = 0;
  long spos  = 0;
  long epos  = start->pos;
  const char* tag   = NULL;
  const char* value = NULL;
  ParsingObject po  = NULL;
  while (mark < ctx->logStackSize) {
    cur = ctx->logStack;
    ctx->logStack = ctx->logStack->next;
    ctx->logStackSize--;
#if NEZ_DEBUG
    fprintf(stderr, "Log[%d] type=%d,cur=%p next=%p\n", commitCount, cur->type, cur, cur->next);
#endif
    switch(cur->type) {
      case LazyLink_T: {
        if(cur->pos == -1) {
          cur->pos = objectSize;
          objectSize++;
        }
        else if(!(cur->pos < objectSize)) {
          objectSize = cur->pos + 1;
        }
        break;
      }
      case LazyCapture_T: {
        epos = cur->pos;
        goto L_unused;
      }
      case LazyLeftJoin_T: {
        po = commitNode(ctx, start, cur, objectSize, spos, epos, tag, value, po);
        start = cur;
        spos = cur->pos;
        epos = spos;
        tag = NULL;
        value = NULL;
        objectSize = 1;
        goto L_unused;
      }
      case LazyNew_T: {
        spos = cur->pos;
        goto L_unused;
      }
      case LazyTag_T: {
        tag = cur->value;
        goto L_unused;
      }
      case LazyValue_T: {
        value = cur->value;
        goto L_unused;
      }
      default: {
    L_unused:
        break;
      }
    }
  }
  po = commitNode(ctx, start, cur, objectSize, spos, epos, tag, value, po);
  //nez_abortLog(ctx, mark);
  return po;
  //nez_pushDataLog(ctx, LazyLink_T, NULL, index, NULL, po);
}

void nez_abortLog(ParsingContext ctx, int mark) {
  int size = ctx->logStackSize;
  ctx->logStackSize = mark;
  while (mark < size--) {
    ParsingLog l = ctx->logStack;
    ctx->logStack = ctx->logStack->next;
    nez_unusedLog(ctx, l);
  }
}

int nez_markLogStack(ParsingContext ctx) {
  return ctx->logStackSize;
}

void createMemoTable(ParsingContext ctx, size_t size) {
  ctx->memo_table = malloc(sizeof(struct MemoTable));
  ctx->memo_table->size = 32 * (size + 1);
  ctx->memo_table->memoArray = malloc(sizeof(struct MemoEntry) * ctx->memo_table->size);
  ctx->memo_table->shift = (int)(log(ctx->memo_table->size) / log(2.0)) + 1;
  for(size_t i = 0; i < ctx->memo_table->size; i++) {
    ctx->memo_table->memoArray[i] = malloc(sizeof(struct MemoEntry));
    ctx->memo_table->memoArray[i]->key = -1;
    ctx->memo_table->memoArray[i]->consumed = NULL;
    ctx->memo_table->memoArray[i]->left = NULL;
  }
}

long longkey(long pos, int memoPoint, int shift) {
  return ((pos << shift) | memoPoint) & 0x7fffffffffffffffL;
}

void nez_setMemo(ParsingContext ctx, char* pos, int memoPoint, int r) {
  long key = longkey(pos - ctx->inputs, memoPoint, ctx->memo_table->shift);
  int hash = (int)(key % ctx->memo_table->size);
  MemoEntry m = ctx->memo_table->memoArray[hash];
  m->key = key;
  m->left = nez_setObject_(ctx, m->left, ctx->left);
  m->consumed = ctx->cur;
  m->r = r;
  ctx->memo_table->memoStored++;
#if NEZ_DEBUG
  fprintf(stderr, "setMemo(key:%ld, pos:%ld, consumed:%ld)\n", key, pos - ctx->inputs, m->consumed - pos);
#endif
}

MemoEntry nez_getMemo(ParsingContext ctx, char* pos, int memoPoint) {
  long key = longkey(pos - ctx->inputs, memoPoint, ctx->memo_table->shift);
  int hash = (int)(key % ctx->memo_table->size);
  MemoEntry m = ctx->memo_table->memoArray[hash];
  if(m->key == key) {
#if NEZ_DEBUG
    fprintf(stderr, "memoHit(key:%ld, pos:%ld, consumed:%ld)\n", key, ctx->cur - ctx->inputs, m->consumed - ctx->cur );
#endif
    ctx->memo_table->memoHit++;
    return m;
  }
#if NEZ_DEBUG
  fprintf(stderr, "memoMiss(key:%ld)\n", key);
#endif
  ctx->memo_table->memoMiss++;
  return NULL;
}

void nez_log(ParsingContext ctx, const char* input_file, const char* grammar, int ruleCount, uint64_t latency, const char* opt) {
  FILE *fp;

  if ((fp = fopen("nez_c_log.csv", "a")) == NULL) {
    fprintf(stderr, "file open error!!\n");
    exit(EXIT_FAILURE);
  }

  /*Date*/
  time_t timer;
  struct tm *local;
  local = localtime(&timer);
  fprintf(fp, "%4d/%2d/%2d %2d:%2d:%2d,", local->tm_year + 1900, local->tm_mon + 1, local->tm_mday, local->tm_hour, local->tm_min, local->tm_sec);

  /*Parser*/
  fprintf(fp, "CNez,");

  /*Config*/
  fprintf(fp, "Config,%s,", opt);

  /*Grammar*/
  fprintf(fp, "Grammar,%s,", grammar);

  /*Production Count*/
  fprintf(fp, "ProductionCount,%d,", ruleCount);

  /*Input File*/
  int len = strlen(input_file);
  int i;
  for(i = len - 1; i >= 0; i--) {
    if(input_file[i] == '/') {
      i++;
      break;
    }
  }
  char input[len - i + 1];
  strncpy(input, input_file+i, len - i);
  input[len - i] = 0;
  fprintf(fp, "Input File,%s,", input);

  /*Input File Size*/
  fprintf(fp, "Input File Size,%zu,", ctx->input_size);

  /*Latency*/
  fprintf(fp, "Latency[ms],%llu,", latency);

  /*Throughput*/
  double throughput = ctx->input_size / 1024.0;
  throughput = throughput * 1000.0 / (double)latency;
  fprintf(fp, "Throughput[KiB/s],%f,", throughput);

  fprintf(fp, "ChoiceCount,%llu,", ctx->choiceCount);

  /*Memo*/
  fprintf(fp, "MemoStored,%lu,", ctx->memo_table->memoStored);
  fprintf(fp, "MemoHit,%lu,", ctx->memo_table->memoHit);
  fprintf(fp, "MemoMiss,%lu\n", ctx->memo_table->memoMiss);

  fclose(fp);
}
