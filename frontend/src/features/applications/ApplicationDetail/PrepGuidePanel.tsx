import { useState } from 'react'
import { Code2, HelpCircle, MessagesSquare, Sparkles } from 'lucide-react'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import type { InterviewPrep, InterviewQuestion } from '@/features/interviewPrep/api'

function QuestionCard({ question }: { question: InterviewQuestion }) {
  const [showAnswer, setShowAnswer] = useState(false)

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <p className="text-sm text-slate-800">{question.question}</p>
      {question.suggestedAnswer && (
        <div className="mt-2">
          <button
            type="button"
            className="text-xs font-medium text-blue-600 hover:underline"
            onClick={() => setShowAnswer((v) => !v)}
          >
            {showAnswer ? 'Hide suggested answer' : 'Show suggested answer'}
          </button>
          {showAnswer && (
            <div className="mt-2 rounded-md border-l-2 border-slate-300 bg-slate-50 p-3 text-sm italic leading-relaxed text-slate-600">
              {question.suggestedAnswer}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function QuestionList({ questions }: { questions: InterviewQuestion[] }) {
  return (
    <div className="space-y-2 pt-1">
      {questions.map((q) => (
        <QuestionCard key={q.id} question={q} />
      ))}
    </div>
  )
}

export function PrepGuidePanel({ prep }: { prep: InterviewPrep }) {
  const total =
    prep.technicalQuestions.length + prep.behavioralQuestions.length + prep.questionsToAsk.length

  if (total === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white py-12 text-center">
        <Sparkles className="mb-2 h-6 w-6 text-slate-400" />
        <p className="text-sm text-slate-500">
          No questions yet — generate the prep guide above to see them here.
        </p>
      </div>
    )
  }

  return (
    <Accordion type="multiple" defaultValue={['technical', 'behavioral', 'ask']}>
      <AccordionItem value="technical">
        <AccordionTrigger>
          <span className="inline-flex items-center gap-2">
            <Code2 className="h-4 w-4 text-slate-500" /> Technical Questions (
            {prep.technicalQuestions.length})
          </span>
        </AccordionTrigger>
        <AccordionContent>
          <QuestionList questions={prep.technicalQuestions} />
        </AccordionContent>
      </AccordionItem>

      <AccordionItem value="behavioral">
        <AccordionTrigger>
          <span className="inline-flex items-center gap-2">
            <MessagesSquare className="h-4 w-4 text-slate-500" /> Behavioral Questions (
            {prep.behavioralQuestions.length})
          </span>
        </AccordionTrigger>
        <AccordionContent>
          <QuestionList questions={prep.behavioralQuestions} />
        </AccordionContent>
      </AccordionItem>

      <AccordionItem value="ask">
        <AccordionTrigger>
          <span className="inline-flex items-center gap-2">
            <HelpCircle className="h-4 w-4 text-slate-500" /> Questions to Ask (
            {prep.questionsToAsk.length})
          </span>
        </AccordionTrigger>
        <AccordionContent>
          <QuestionList questions={prep.questionsToAsk} />
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  )
}
