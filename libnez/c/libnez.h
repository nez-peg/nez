#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <sys/time.h> // gettimeofday

#ifndef LIBNEZ_H
#define LIBNEZ_H

struct ParsingObject {
  int refc; // referencing counting gc
  int child_size;
  struct ParsingObject **child;
  struct ParsingObject *parent;
  long start_pos;
  long end_pos;
  const char *tag;
  const char *value;
};

struct MemoTable {
  int shift;
  unsigned long memoHit;
  unsigned long memoMiss;
  unsigned long memoStored;
  size_t size;
  struct MemoEntry** memoArray;
};

struct MemoEntry {
  long key;
  char *consumed;
  struct ParsingObject *left;
  int r;
};

#define LazyLink_T 0
#define LazyCapture_T 1
#define LazyTag_T 2
#define LazyValue_T	3
#define LazyLeftJoin_T 4
#define LazyNew_T 5

struct ParsingLog {
  int type;
  const char* value;
  long pos;
  struct ParsingObject *po;
  struct ParsingLog *next;
};

struct MemoryPool {
  struct ParsingObject *object_pool;
  struct ParsingLog *log_pool;
  size_t oidx;
  size_t lidx;
  size_t init_size;
};

union StackEntry {
  const char* pos;
  const struct NezVMInstruction *func;
};

struct ParsingContext {
  char *inputs;
  char *cur;
  size_t input_size;
  long pos;
  struct ParsingObject *left;
  struct ParsingObject *unusedObject;

  int logStackSize;
  struct ParsingLog *logStack;
  struct ParsingLog *unusedLog;

  size_t flags_size;
  int* flags;

  struct MemoTable *memo_table;

  uint64_t choiceCount;

};

typedef struct ParsingObject *ParsingObject;
typedef struct ParsingLog *ParsingLog;
typedef struct ParsingContext *ParsingContext;
typedef struct MemoryPool *MemoryPool;
typedef union StackEntry* StackEntry;
typedef struct MemoEntry* MemoEntry;

int level;
static void dump_func(const char* name) {
  level++;
  for(int i = 0; i < level; i++) {
    fprintf(stderr, "  ");
  }
  fprintf(stderr, "%s:%d\n", name, level);
}

static void dec() {
  level--;
}

static uint64_t timer() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

extern MemoryPool nez_CreateMemoryPool(MemoryPool mpool, size_t init_size);
extern void MemoryPool_Reset(MemoryPool mpool);
extern void nez_DisposeMemoryPool(MemoryPool mpool);

static inline ParsingObject MemoryPool_AllocParsingObject(MemoryPool mpool) {
  assert(mpool->oidx < mpool->init_size);
  return &mpool->object_pool[mpool->oidx++];
}

static inline ParsingLog MemoryPool_AllocParsingLog(MemoryPool mpool) {
  assert(mpool->lidx < mpool->init_size);
  return &mpool->log_pool[mpool->lidx++];
}

void nez_consume(ParsingContext ctx);
int nez_unconsume_check(ParsingContext ctx, long pos);
void nez_backtrack(ParsingContext ctx, long pos);
static inline int nez_match(ParsingContext ctx, char c) {
  if(ctx->pos++ > ctx->input_size) {
    fprintf(stderr, "error\n");
    return 0;
  }
  ctx->pos--;
  return ctx->inputs[ctx->pos] == c;
}
int nez_not_match(ParsingContext ctx, char c);

#define PARSING_CONTEXT_MAX_STACK_LENGTH 1024
ParsingContext nez_CreateParsingContext(const char *filename);
void nez_DisposeParsingContext(ParsingContext ctx);
void nez_DisposeObject(ParsingObject pego);

void nez_PrintErrorInfo(const char *errmsg);
void dump_pego(ParsingObject *pego, char *source, int level);

void nez_setObject(ParsingContext ctx, ParsingObject *var, ParsingObject o);
ParsingObject nez_setObject_(ParsingContext ctx, ParsingObject var, ParsingObject o);
ParsingObject nez_newObject(ParsingContext ctx, const char *start);
ParsingObject nez_newObject_(ParsingContext ctx, long start, long end,
                             const char* tag, const char* value);
void nez_pushDataLog(ParsingContext ctx, int type, long pos,
                     int index, const char* value, ParsingObject po);
ParsingObject nez_commitLog(ParsingContext ctx, int mark);
void nez_abortLog(ParsingContext ctx, int mark);
int nez_markLogStack(ParsingContext ctx);

void createMemoTable(ParsingContext ctx, size_t size);
void nez_setMemo(ParsingContext ctx, char* pos, int memoPoint, int r);
MemoEntry nez_getMemo(ParsingContext ctx, char* pos, int memoPoint);

void nez_log(ParsingContext ctx, const char* input_file, const char* grammar, int ruleCount, uint64_t latency, const char* opt);

#endif