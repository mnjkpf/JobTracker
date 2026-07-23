import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { cvApi } from './api'
import type {
  AddSkillRequest,
  EducationRequest,
  ExperienceRequest,
  LanguageRequest,
  ProjectRequest,
  UpdateProfileRequest,
  UpdateSkillRequest,
} from './types'

const CV_KEY = ['cv', 'me'] as const

function statusOf(error: unknown): number | undefined {
  return (error as { response?: { status?: number } })?.response?.status
}

function errorMessage(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { detail?: string } } }
  return e?.response?.data?.detail ?? fallback
}

export const useCv = () => {
  return useQuery({
    queryKey: CV_KEY,
    queryFn: () => cvApi.getMy(),
    retry: (failureCount, error) => (statusOf(error) === 404 ? false : failureCount < 1),
  })
}

function useCvMutation<TVariables>(
  mutationFn: (vars: TVariables) => Promise<unknown>,
  successMessage: string,
  failMessage: string,
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CV_KEY })
      toast.success(successMessage)
    },
    onError: (error) => toast.error(errorMessage(error, failMessage)),
  })
}

export const useUpdateProfile = () =>
  useCvMutation((data: UpdateProfileRequest) => cvApi.updateProfile(data), 'Profile updated', 'Failed to update profile')

export const useUploadCv = () =>
  useCvMutation((file: File) => cvApi.uploadCv(file), 'CV extracted successfully', 'Failed to extract CV')

export const useAddExperience = () =>
  useCvMutation((data: ExperienceRequest) => cvApi.addExperience(data), 'Experience added', 'Failed to add experience')

export const useUpdateExperience = (id: string) =>
  useCvMutation((data: ExperienceRequest) => cvApi.updateExperience(id, data), 'Experience updated', 'Failed to update experience')

export const useDeleteExperience = () =>
  useCvMutation((id: string) => cvApi.deleteExperience(id), 'Experience deleted', 'Failed to delete experience')

export const useAddEducation = () =>
  useCvMutation((data: EducationRequest) => cvApi.addEducation(data), 'Education added', 'Failed to add education')

export const useUpdateEducation = (id: string) =>
  useCvMutation((data: EducationRequest) => cvApi.updateEducation(id, data), 'Education updated', 'Failed to update education')

export const useDeleteEducation = () =>
  useCvMutation((id: string) => cvApi.deleteEducation(id), 'Education deleted', 'Failed to delete education')

export const useAddSkill = () =>
  useCvMutation((data: AddSkillRequest) => cvApi.addSkill(data), 'Skill added', 'Failed to add skill')

export const useUpdateSkill = (id: string) =>
  useCvMutation((data: UpdateSkillRequest) => cvApi.updateSkill(id, data), 'Skill updated', 'Failed to update skill')

export const useDeleteSkill = () =>
  useCvMutation((id: string) => cvApi.deleteSkill(id), 'Skill removed', 'Failed to remove skill')

export const useAddProject = () =>
  useCvMutation((data: ProjectRequest) => cvApi.addProject(data), 'Project added', 'Failed to add project')

export const useUpdateProject = (id: string) =>
  useCvMutation((data: ProjectRequest) => cvApi.updateProject(id, data), 'Project updated', 'Failed to update project')

export const useDeleteProject = () =>
  useCvMutation((id: string) => cvApi.deleteProject(id), 'Project deleted', 'Failed to delete project')

export const useAddLanguage = () =>
  useCvMutation((data: LanguageRequest) => cvApi.addLanguage(data), 'Language added', 'Failed to add language')

export const useUpdateLanguage = (id: string) =>
  useCvMutation((data: LanguageRequest) => cvApi.updateLanguage(id, data), 'Language updated', 'Failed to update language')

export const useDeleteLanguage = () =>
  useCvMutation((id: string) => cvApi.deleteLanguage(id), 'Language deleted', 'Failed to delete language')
